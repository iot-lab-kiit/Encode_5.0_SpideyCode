import { NormalizedRect } from '../types';

const windowCache = new Map<string, NormalizedRect>();

/**
 * Analyzes an image's pixel data to locate the transparent aperture window where the photo should sit.
 */
export function detectTransparentWindow(
  image: HTMLImageElement | ImageBitmap,
  cacheKey?: string
): NormalizedRect | null {
  if (cacheKey && windowCache.has(cacheKey)) {
    return windowCache.get(cacheKey)!;
  }

  const canvas = document.createElement('canvas');
  const w = image.width;
  const h = image.height;
  if (w <= 0 || h <= 0) return null;

  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext('2d');
  if (!ctx) return null;

  ctx.drawImage(image, 0, 0);
  const imgData = ctx.getImageData(0, 0, w, h);
  const data = imgData.data;

  // Sample grid to find transparent pixels (alpha < 30)
  let minX = w;
  let minY = h;
  let maxX = 0;
  let maxY = 0;
  let transparentCount = 0;

  // Step sampling for performance
  const step = 4;
  for (let y = 0; y < h; y += step) {
    for (let x = 0; x < w; x += step) {
      const alphaIndex = (y * w + x) * 4 + 3;
      const alpha = data[alphaIndex];

      if (alpha < 30) {
        transparentCount++;
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }
  }

  if (transparentCount < 50 || minX >= maxX || minY >= maxY) {
    return null;
  }

  const result: NormalizedRect = {
    left: Math.max(0, minX / w),
    top: Math.max(0, minY / h),
    width: Math.min(1, (maxX - minX) / w),
    height: Math.min(1, (maxY - minY) / h),
  };

  if (cacheKey) {
    windowCache.set(cacheKey, result);
  }

  return result;
}
