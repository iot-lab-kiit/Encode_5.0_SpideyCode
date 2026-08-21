import { FilesetResolver, FaceLandmarker } from '@mediapipe/tasks-vision';
import { TransformedFaceData } from '../types';

export const SPIDEY_MASK_REFERENCE = {
  width: 1024,
  height: 1381,
  faceWidthRatio: 0.90,
  referenceMaskFaceWidth: 1024 * 0.90, // ~921.6px
  leftEye: { x: 285.4, y: 748.4 },
  rightEye: { x: 755.2, y: 773.6 },
  eyeCenter: { x: (285.4 + 755.2) / 2, y: (748.4 + 773.6) / 2 }, // { x: 520.3, y: 761.0 }
  maskEyeAngleDeg: Math.atan2(773.6 - 748.4, 755.2 - 285.4) * (180 / Math.PI), // ~3.07 deg
};

interface SmoothedFace {
  box: { left: number; top: number; width: number; height: number };
  leftEye: { x: number; y: number } | null;
  rightEye: { x: number; y: number } | null;
  angle: number;
  lastSeenMs: number;
}

class MediaPipeManager {
  private landmarker: FaceLandmarker | null = null;
  private isInitializing = false;
  private smoothedFaces: SmoothedFace[] = [];
  private readonly SMOOTHING_FACTOR = 0.35;
  private readonly STALE_FACE_TIMEOUT_MS = 250;

  async initialize(): Promise<boolean> {
    if (this.landmarker) return true;
    if (this.isInitializing) return false;

    this.isInitializing = true;
    try {
      const vision = await FilesetResolver.forVisionTasks(
        'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm'
      );

      this.landmarker = await FaceLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task',
          delegate: 'GPU',
        },
        runningMode: 'VIDEO',
        numFaces: 3,
        minFaceDetectionConfidence: 0.5,
        minFacePresenceConfidence: 0.5,
        minTrackingConfidence: 0.5,
      });

      this.isInitializing = false;
      return true;
    } catch (err) {
      console.warn('GPU delegate failed or network error, falling back to CPU delegate:', err);
      try {
        const vision = await FilesetResolver.forVisionTasks(
          'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm'
        );
        this.landmarker = await FaceLandmarker.createFromOptions(vision, {
          baseOptions: {
            modelAssetPath:
              'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task',
            delegate: 'CPU',
          },
          runningMode: 'VIDEO',
          numFaces: 3,
        });
        this.isInitializing = false;
        return true;
      } catch (fallbackErr) {
        console.error('Failed to initialize FaceLandmarker:', fallbackErr);
        this.isInitializing = false;
        return false;
      }
    }
  }

  detectFaces(
    video: HTMLVideoElement,
    previewWidth: number,
    previewHeight: number,
    isFrontCamera: boolean = true
  ): TransformedFaceData[] {
    if (!this.landmarker || video.readyState < 2 || previewWidth <= 0 || previewHeight <= 0) {
      return [];
    }

    const timestamp = performance.now();
    const results = this.landmarker.detectForVideo(video, timestamp);
    const now = Date.now();

    if (!results || !results.faceLandmarks || results.faceLandmarks.length === 0) {
      // Grace period for momentary drops
      this.smoothedFaces = this.smoothedFaces.filter(
        (f) => now - f.lastSeenMs < this.STALE_FACE_TIMEOUT_MS
      );
      return this.smoothedFaces.map((f) => ({
        boundingBox: f.box,
        leftEye: f.leftEye,
        rightEye: f.rightEye,
        eyeAngleDeg: f.angle,
      }));
    }

    const videoW = video.videoWidth || 640;
    const videoH = video.videoHeight || 480;

    // Cover crop scaling to match video display viewport
    const scale = Math.max(previewWidth / videoW, previewHeight / videoH);
    const scaledW = videoW * scale;
    const scaledH = videoH * scale;
    const offsetX = (scaledW - previewWidth) / 2;
    const offsetY = (scaledH - previewHeight) / 2;

    const currentFrameFaces: TransformedFaceData[] = [];

    results.faceLandmarks.forEach((landmarks, idx) => {
      // Landmark 33 (left outer), 133 (left inner) -> midpoint
      const rawLeftEyeX = (landmarks[33].x + landmarks[133].x) / 2;
      const rawLeftEyeY = (landmarks[33].y + landmarks[133].y) / 2;

      // Landmark 362 (right inner), 263 (right outer) -> midpoint
      const rawRightEyeX = (landmarks[362].x + landmarks[263].x) / 2;
      const rawRightEyeY = (landmarks[362].y + landmarks[263].y) / 2;

      // Bounding box bounds from key landmarks
      let minNormX = 1,
        maxNormX = 0,
        minNormY = 1,
        maxNormY = 0;
      landmarks.forEach((pt) => {
        if (pt.x < minNormX) minNormX = pt.x;
        if (pt.x > maxNormX) maxNormX = pt.x;
        if (pt.y < minNormY) minNormY = pt.y;
        if (pt.y > maxNormY) maxNormY = pt.y;
      });

      const transformPt = (normX: number, normY: number) => {
        const pixelX = normX * videoW * scale - offsetX;
        const pixelY = normY * videoH * scale - offsetY;
        const screenX = isFrontCamera ? previewWidth - pixelX : pixelX;
        return { x: screenX, y: pixelY };
      };

      const ptLeft = transformPt(rawLeftEyeX, rawLeftEyeY);
      const ptRight = transformPt(rawRightEyeX, rawRightEyeY);

      // In mirrored front camera, screen left is the person's physical right eye
      const finalLeftEye = ptLeft.x <= ptRight.x ? ptLeft : ptRight;
      const finalRightEye = ptLeft.x <= ptRight.x ? ptRight : ptLeft;

      const topCorner = transformPt(minNormX, minNormY);
      const bottomCorner = transformPt(maxNormX, maxNormY);

      const boxLeft = Math.min(topCorner.x, bottomCorner.x);
      const boxTop = Math.min(topCorner.y, bottomCorner.y);
      const boxWidth = Math.abs(bottomCorner.x - topCorner.x);
      const boxHeight = Math.abs(bottomCorner.y - topCorner.y);

      const dx = finalRightEye.x - finalLeftEye.x;
      const dy = finalRightEye.y - finalLeftEye.y;
      const rawAngleDeg = Math.atan2(dy, dx) * (180 / Math.PI);

      // Exponential Smoothing with previous face state
      const prior = this.smoothedFaces[idx];
      const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

      const smoothedBox = prior
        ? {
            left: lerp(prior.box.left, boxLeft, this.SMOOTHING_FACTOR),
            top: lerp(prior.box.top, boxTop, this.SMOOTHING_FACTOR),
            width: lerp(prior.box.width, boxWidth, this.SMOOTHING_FACTOR),
            height: lerp(prior.box.height, boxHeight, this.SMOOTHING_FACTOR),
          }
        : { left: boxLeft, top: boxTop, width: boxWidth, height: boxHeight };

      const smoothedLeft = prior && prior.leftEye
        ? {
            x: lerp(prior.leftEye.x, finalLeftEye.x, this.SMOOTHING_FACTOR),
            y: lerp(prior.leftEye.y, finalLeftEye.y, this.SMOOTHING_FACTOR),
          }
        : finalLeftEye;

      const smoothedRight = prior && prior.rightEye
        ? {
            x: lerp(prior.rightEye.x, finalRightEye.x, this.SMOOTHING_FACTOR),
            y: lerp(prior.rightEye.y, finalRightEye.y, this.SMOOTHING_FACTOR),
          }
        : finalRightEye;

      const smoothedAngle = prior
        ? lerp(prior.angle, rawAngleDeg, this.SMOOTHING_FACTOR)
        : rawAngleDeg;

      this.smoothedFaces[idx] = {
        box: smoothedBox,
        leftEye: smoothedLeft,
        rightEye: smoothedRight,
        angle: smoothedAngle,
        lastSeenMs: now,
      };

      currentFrameFaces.push({
        boundingBox: smoothedBox,
        leftEye: smoothedLeft,
        rightEye: smoothedRight,
        eyeAngleDeg: smoothedAngle,
      });
    });

    return currentFrameFaces;
  }
}

export const mediaPipeManager = new MediaPipeManager();
