import { FilterType, TransformedFaceData, NormalizedRect } from '../types';
import {
  FRAME_DEFINITIONS,
  DEFAULT_BRANDING_CONFIG,
  CORNER_LOGOS,
  EVENT_BADGE_LOGO,
  SPIDEY_MASK_SRC,
} from './FrameDefinitions';
import { detectTransparentWindow } from './FrameWindowDetector';
import { SPIDEY_MASK_REFERENCE } from './MediaPipeManager';

interface ImageCache {
  [src: string]: HTMLImageElement;
}

const imageCache: ImageCache = {};

export async function loadImage(src: string): Promise<HTMLImageElement> {
  if (imageCache[src] && imageCache[src].complete) {
    return imageCache[src];
  }
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      imageCache[src] = img;
      resolve(img);
    };
    img.onerror = (e) => reject(e);
    img.src = src;
  });
}

/**
 * Creates the high-resolution composed poster replicating ImageCompositionUtils from Kotlin.
 */
export async function createComposedPoster(
  photoSource: HTMLCanvasElement | HTMLVideoElement | HTMLImageElement,
  selectedFilter: FilterType,
  isMaskEnabled: boolean,
  facesSnapshot: TransformedFaceData[],
  previewWidth: number,
  previewHeight: number,
  isFrontCamera: boolean = true
): Promise<string> {
  const frameDef = FRAME_DEFINITIONS[selectedFilter];
  const frameImg = await loadImage(frameDef.assetPath);
  const frameW = frameImg.naturalWidth || frameImg.width || 1080;
  const frameH = frameImg.naturalHeight || frameImg.height || 1920;

  // Resolve transparent window rect
  const detectedWindow = detectTransparentWindow(frameImg, frameDef.assetPath);
  const activeWindow: NormalizedRect = detectedWindow || frameDef.fallbackWindow;

  const winX = Math.round(activeWindow.left * frameW);
  const winY = Math.round(activeWindow.top * frameH);
  const winW = Math.round(activeWindow.width * frameW);
  const winH = Math.round(activeWindow.height * frameH);

  // Main high-res canvas
  const canvas = document.createElement('canvas');
  canvas.width = frameW;
  canvas.height = frameH;
  const ctx = canvas.getContext('2d', { alpha: false });
  if (!ctx) throw new Error('Canvas 2D context not available');

  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';

  // Fill black base
  ctx.fillStyle = '#0E1320';
  ctx.fillRect(0, 0, frameW, frameH);

  // 1. Draw photo center-cropped into photo window (FILL_CENTER equivalent)
  const srcW =
    'videoWidth' in photoSource
      ? photoSource.videoWidth
      : 'naturalWidth' in photoSource
      ? photoSource.naturalWidth
      : photoSource.width;
  const srcH =
    'videoHeight' in photoSource
      ? photoSource.videoHeight
      : 'naturalHeight' in photoSource
      ? photoSource.naturalHeight
      : photoSource.height;

  if (srcW > 0 && srcH > 0 && winW > 0 && winH > 0) {
    const photoRatio = srcW / srcH;
    const winRatio = winW / winH;

    let cropX = 0;
    let cropY = 0;
    let cropW = srcW;
    let cropH = srcH;

    if (photoRatio > winRatio) {
      cropW = srcH * winRatio;
      cropX = (srcW - cropW) / 2;
    } else {
      cropH = srcW / winRatio;
      cropY = (srcH - cropH) / 2;
    }

    ctx.save();
    // Clip strictly to window
    ctx.beginPath();
    ctx.rect(winX, winY, winW, winH);
    ctx.clip();

    if (isFrontCamera) {
      // Mirror horizontally inside window for selfie perspective
      ctx.translate(winX + winW, winY);
      ctx.scale(-1, 1);
      ctx.drawImage(photoSource, cropX, cropY, cropW, cropH, 0, 0, winW, winH);
    } else {
      ctx.drawImage(photoSource, cropX, cropY, cropW, cropH, winX, winY, winW, winH);
    }
    ctx.restore();
  }

  // 2. Draw Face Mask if enabled and faces detected
  if (isMaskEnabled && facesSnapshot.length > 0 && previewWidth > 0 && previewHeight > 0) {
    try {
      const maskImg = await loadImage(SPIDEY_MASK_SRC);
      ctx.save();
      // Clip to window so mask doesn't overflow outside the photo frame
      ctx.beginPath();
      ctx.rect(winX, winY, winW, winH);
      ctx.clip();

      for (const face of facesSnapshot) {
        if (face.leftEye && face.rightEye) {
          const normEyeX = (face.leftEye.x + face.rightEye.x) / 2 / previewWidth;
          const normEyeY = (face.leftEye.y + face.rightEye.y) / 2 / previewHeight;

          const normFaceWidth = face.boundingBox.width / previewWidth;
          const eyeCenterX = winX + normEyeX * winW;
          const eyeCenterY = winY + normEyeY * winH;

          const faceWidthOnComposite = normFaceWidth * winW;
          const maskScale =
            faceWidthOnComposite / SPIDEY_MASK_REFERENCE.referenceMaskFaceWidth;
          const deltaAngleDeg = face.eyeAngleDeg - SPIDEY_MASK_REFERENCE.maskEyeAngleDeg;

          ctx.save();
          ctx.translate(eyeCenterX, eyeCenterY);
          ctx.rotate((deltaAngleDeg * Math.PI) / 180);
          ctx.scale(maskScale, maskScale);

          ctx.drawImage(
            maskImg,
            -SPIDEY_MASK_REFERENCE.eyeCenter.x,
            -SPIDEY_MASK_REFERENCE.eyeCenter.y
          );
          ctx.restore();
        }
      }
      ctx.restore();
    } catch (e) {
      console.warn('Failed to render mask overlay on photo capture:', e);
    }
  }

  // 3. Draw Frame Overlay
  ctx.drawImage(frameImg, 0, 0, frameW, frameH);

  // 4. Draw Branding Overlay if required
  if (frameDef.showBrandingOverlay) {
    await drawBrandingLayer(ctx, frameW, frameH, {
      left: winX,
      top: winY,
      width: winW,
      height: winH,
      badgeCorner: frameDef.badgeCorner,
    });
  }

  return canvas.toDataURL('image/jpeg', 0.96);
}

interface WindowBounds {
  left: number;
  top: number;
  width: number;
  height: number;
  badgeCorner: 'LEFT' | 'RIGHT';
}

async function drawBrandingLayer(
  ctx: CanvasRenderingContext2D,
  frameW: number,
  frameH: number,
  windowBounds: WindowBounds
) {
  const config = DEFAULT_BRANDING_CONFIG;
  const logoSize = frameW * config.logoSizeRatio;
  const logoGap = frameW * config.logoGapRatio;
  const marginTop = frameH * config.logoMarginTopRatio;
  const marginEnd = frameW * config.logoMarginEndRatio;
  const totalWidth = CORNER_LOGOS.length * logoSize + (CORNER_LOGOS.length - 1) * logoGap;

  let x = frameW - marginEnd - totalWidth;

  // Draw 3 Society Circular Logos (AlgoZenith, IoT Lab, KSAC)
  for (const logoPath of CORNER_LOGOS) {
    try {
      const logoImg = await loadImage(logoPath);
      ctx.save();
      ctx.beginPath();
      ctx.arc(x + logoSize / 2, marginTop + logoSize / 2, logoSize / 2, 0, Math.PI * 2);
      ctx.closePath();
      ctx.clip();
      ctx.drawImage(logoImg, x, marginTop, logoSize, logoSize);
      ctx.restore();
    } catch (e) {
      console.warn('Failed to load logo:', logoPath, e);
    }
    x += logoSize + logoGap;
  }

  // Draw Event Badge
  try {
    const badgeImg = await loadImage(EVENT_BADGE_LOGO);
    const badgeWidth = frameW * config.badgeWidthRatio;
    const aspect = badgeImg.naturalWidth / badgeImg.naturalHeight || 2.5;
    const badgeHeight = badgeWidth / aspect;

    const cornerX =
      windowBounds.badgeCorner === 'RIGHT'
        ? windowBounds.left + windowBounds.width
        : windowBounds.left;
    const cornerY = windowBounds.top + windowBounds.height;

    const offsetX =
      windowBounds.badgeCorner === 'RIGHT'
        ? cornerX - badgeWidth * config.badgeInsetRatio
        : cornerX - badgeWidth * (1 - config.badgeInsetRatio);
    const offsetY = cornerY - badgeHeight * 0.5;

    ctx.drawImage(badgeImg, offsetX, offsetY, badgeWidth, badgeHeight);
  } catch (e) {
    console.warn('Failed to load event badge:', e);
  }
}
