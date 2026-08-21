export type FilterType =
  | 'CLASSIC_MASK'
  | 'WEB_SHOOTER'
  | 'SPIDEY_SENSE'
  | 'SPIDER_VERSE'
  | 'EVENT_SQUAD'
  | 'SPIDEY_PARTY'
  | 'GHOST_SPIDER';

export type BadgeCorner = 'LEFT' | 'RIGHT';

export interface NormalizedRect {
  left: number;
  top: number;
  width: number;
  height: number;
}

export interface FrameDefinition {
  id: FilterType;
  name: string;
  assetPath: string;
  fallbackWindow: NormalizedRect;
  showBrandingOverlay: boolean;
  badgeCorner: BadgeCorner;
  description?: string;
}

export interface TransformedFaceData {
  boundingBox: {
    left: number;
    top: number;
    width: number;
    height: number;
  };
  leftEye: { x: number; y: number } | null;
  rightEye: { x: number; y: number } | null;
  eyeAngleDeg: number;
}

export type FlashMode = 'OFF' | 'AUTO' | 'ON';

export type AppScreen = 'SPLASH' | 'GEAR_SELECTION' | 'CAMERA' | 'REVIEW' | 'VIDEO_REVIEW';

export interface BrandingConfig {
  logoSizeRatio: number;
  logoGapRatio: number;
  logoMarginTopRatio: number;
  logoMarginEndRatio: number;
  badgeWidthRatio: number;
  badgeInsetRatio: number;
}
