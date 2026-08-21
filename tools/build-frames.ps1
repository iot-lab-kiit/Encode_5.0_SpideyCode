<#
.SYNOPSIS
    Regenerates every photobooth frame from its source folder under frames/.

.DESCRIPTION
    Each filter (except Classic -- see note below) has its own folder under
    app/src/main/frames/<filter-name>/ containing everything that makes up
    that frame:

      background.png   - the base frame/border art (required)
      sticker_1.png,    - decorative spider/web overlays (optional, any count)
      sticker_2.png, ...
      config.json       - the photo window position/size + where each
                          sticker goes, all as ratios (0.0-1.0) of the
                          background image's own width/height
      preview.png       - written by this script: the final composed frame,
                          so you can see the result without opening the app

    This script is the single source of truth for the app's frame assets.
    Editing files under frames/ and re-running this is the whole workflow --
    it writes the composed result into BOTH frames/<name>/preview.png (for
    you to look at) and app/assets/images/ (what the app actually loads, per
    that filter's config.json "outputAsset").

.HOW TO CHANGE A FILTER
    - Swap the look entirely: replace frames/<name>/background.png with a
      new image (any resolution), then re-run this script.
    - Move/resize the photo window ("bigger camera frame"): edit the
      "window" left/top/width/height numbers in that filter's config.json.
    - Add, remove, resize, or reposition a sticker: edit the "stickers"
      array in config.json. Add a new sticker_N.png file to the folder and
      add a matching entry -- no code changes needed.
    - x/y are the sticker's position as a fraction of the background image's
      width/height. width is the sticker's width as a fraction of the
      background's width (its height follows the sticker's own aspect ratio
      automatically). yMode "Top" anchors y to the sticker's top edge (use
      for things hanging/swinging in from above); "Bottom" anchors y to the
      sticker's bottom edge (use for standing/self-contained characters).
    - "binarize": true snaps any soft/semi-transparent edge pixels on that
      sticker to fully opaque/transparent -- fixes a faint "ghost box"
      background some sticker exports leave behind. Leave it off for PNGs
      you already know have clean alpha.

.ADDING A BRAND-NEW FILTER
    1. Create app/src/main/frames/<new_name>/ with a background.png, any
       stickers, and a config.json (copy an existing one as a template).
    2. Run this script -- it picks up every folder under app/src/main/frames/
       automatically.
    3. Add a matching enum value + frameDefinition (assetPath = the
       "outputAsset" you chose, fallbackWindow = the same "window" numbers)
       in app/src/main/java/in/iot/spidey_code/data/model/FilterType.kt.
       It'll then automatically appear in Gear Selection and the in-camera
       filter carousel (both iterate every FilterType).

.NOTE ON CLASSIC
    The Classic filter is intentionally NOT part of this system. It uses
    app/assets/images/frame_1_2.png completely unmodified -- its own art
    already has AlgoZenith/KIIT/KSAC/IoT logos and the event title baked in
    (see FilterType.showBrandingOverlay). Don't add a frames/classic/ folder.

.NOTE ON LOGOS/BADGE
    The small corner logos and the "ENCODE 5.0 X ZENITHCUP" badge are NOT
    part of any background/sticker here -- they're a separate shared layer
    drawn on top of every filter (except Classic) at runtime, sized from
    app/assets/branding.json (edit that directly -- no script needed, just
    rebuild the app), so they always look identical no matter which frame is
    selected. The drawing code itself lives in:
      - app/src/main/java/in/iot/spidey_code/view/components/BrandingOverlay.kt (live preview)
      - app/src/main/java/in/iot/spidey_code/utils/ImageCompositionUtils.kt -> drawBrandingOverlay() (final photo)

.USAGE
    powershell -ExecutionPolicy Bypass -File tools\build-frames.ps1
#>

Add-Type -AssemblyName System.Drawing

$RepoRoot = Split-Path $PSScriptRoot -Parent
$FramesRoot = Join-Path $RepoRoot "app\src\main\frames"
$AssetsRoot = Join-Path $RepoRoot "app\assets"
$WebAssetsRoot = Join-Path $RepoRoot "web\public\assets"

function Get-ArgbLayer {
    param([string]$Path, [switch]$BinarizeAlpha, [int]$Threshold = 160)
    $src = [System.Drawing.Bitmap]::FromFile($Path)
    $w = $src.Width; $h = $src.Height
    $argb = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($argb)
    $g.DrawImage($src, 0, 0, $w, $h)
    $g.Dispose(); $src.Dispose()

    $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
    $data = $argb.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride = $data.Stride
    $bytes = New-Object byte[] ($stride * $h)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)

    if ($BinarizeAlpha) {
        for ($i = 0; $i -lt $bytes.Length; $i += 4) {
            $a = $bytes[$i + 3]
            $bytes[$i + 3] = if ($a -ge $Threshold) { 255 } else { 0 }
        }
        [System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length)
    }
    $argb.UnlockBits($data)
    return @{ Bitmap = $argb; Width = $w; Height = $h }
}

function Punch-Transparent {
    param([System.Drawing.Bitmap]$Bitmap, [int]$Left, [int]$Top, [int]$Right, [int]$Bottom)
    $w = $Bitmap.Width; $h = $Bitmap.Height
    $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
    $data = $Bitmap.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride = $data.Stride
    $bytes = New-Object byte[] ($stride * $h)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    for ($y = $Top; $y -lt $Bottom; $y++) {
        for ($x = $Left; $x -lt $Right; $x++) {
            $idx = $y * $stride + $x * 4
            $bytes[$idx] = 0; $bytes[$idx+1] = 0; $bytes[$idx+2] = 0; $bytes[$idx+3] = 0
        }
    }
    [System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length)
    $Bitmap.UnlockBits($data)
}

function Draw-LayerOnto {
    param([System.Drawing.Graphics]$Graphics, [System.Drawing.Bitmap]$Src, [int]$X, [int]$Y, [int]$W, [int]$H)
    $destRect = New-Object System.Drawing.Rectangle $X, $Y, $W, $H
    $Graphics.DrawImage($Src, $destRect, 0, 0, $Src.Width, $Src.Height, [System.Drawing.GraphicsUnit]::Pixel)
}

$folders = Get-ChildItem -Path $FramesRoot -Directory | Sort-Object Name
foreach ($folder in $folders) {
    $configPath = Join-Path $folder.FullName "config.json"
    if (-not (Test-Path $configPath)) {
        Write-Warning "Skipping $($folder.Name): no config.json"
        continue
    }
    $config = Get-Content $configPath -Raw | ConvertFrom-Json

    $backgroundPath = Join-Path $folder.FullName "background.png"
    if (-not (Test-Path $backgroundPath)) {
        Write-Warning "Skipping $($folder.Name): no background.png"
        continue
    }

    $layer = Get-ArgbLayer -Path $backgroundPath
    $fw = $layer.Width; $fh = $layer.Height

    $win = $config.window
    $l = [int]($win.left * $fw); $t = [int]($win.top * $fh)
    $r = $l + [int]($win.width * $fw); $b = $t + [int]($win.height * $fh)
    Punch-Transparent -Bitmap $layer.Bitmap -Left $l -Top $t -Right $r -Bottom $b

    $g = [System.Drawing.Graphics]::FromImage($layer.Bitmap)
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    foreach ($sticker in $config.stickers) {
        $stickerPath = Join-Path $folder.FullName $sticker.file
        if (-not (Test-Path $stickerPath)) {
            Write-Warning "  Missing sticker $($sticker.file) in $($folder.Name), skipping it"
            continue
        }
        $binarize = [bool]($sticker.binarize)
        $stickerLayer = Get-ArgbLayer -Path $stickerPath -BinarizeAlpha:$binarize -Threshold 160
        $stickerAspect = $stickerLayer.Width / [double]$stickerLayer.Height
        $stickerW = [int]($sticker.width * $fw)
        $stickerH = [int]($stickerW / $stickerAspect)
        $sx = [int]($sticker.x * $fw)
        $sy = if ($sticker.yMode -eq 'Bottom') { [int]($sticker.y * $fh) - $stickerH } else { [int]($sticker.y * $fh) }
        Draw-LayerOnto -Graphics $g -Src $stickerLayer.Bitmap -X $sx -Y $sy -W $stickerW -H $stickerH
        $stickerLayer.Bitmap.Dispose()
    }

    $g.Dispose()

    $previewPath = Join-Path $folder.FullName "preview.png"
    $layer.Bitmap.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $outputPath = Join-Path $AssetsRoot ($config.outputAsset -replace '/', '\')
    $outputDir = Split-Path $outputPath -Parent
    $layer.Bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    if (Test-Path $WebAssetsRoot) {
        $webOutputPath = Join-Path $WebAssetsRoot ($config.outputAsset -replace '/', '\')
        $webOutputDir = Split-Path $webOutputPath -Parent
        if (-not (Test-Path $webOutputDir)) { New-Item -ItemType Directory -Path $webOutputDir -Force | Out-Null }
        $layer.Bitmap.Save($webOutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    }

    $layer.Bitmap.Dispose()
    "Built '$($config.displayName)' -> $previewPath and $outputPath"
}

"ALL FRAMES BUILT"
