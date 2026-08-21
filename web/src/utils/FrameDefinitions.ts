import { FilterType, FrameDefinition, BrandingConfig } from '../types';

export const DEFAULT_BRANDING_CONFIG: BrandingConfig = {
  logoSizeRatio: 0.15,
  logoGapRatio: 0.012,
  logoMarginTopRatio: 0.025,
  logoMarginEndRatio: 0.025,
  badgeWidthRatio: 0.34,
  badgeInsetRatio: 0.85,
};

export const CORNER_LOGOS = [
  '/assets/logos/algozenith_logo.png',
  '/assets/logos/iot_logo.png',
  '/assets/logos/ksac_logo.webp',
];

export const EVENT_BADGE_LOGO = '/assets/logos/encodexzenith_logo.png';
export const SPIDEY_MASK_SRC = '/assets/masks/spidey_mask.png';

export const FRAME_DEFINITIONS: Record<FilterType, FrameDefinition> = {
  CLASSIC_MASK: {
    id: 'CLASSIC_MASK',
    name: 'Classic',
    assetPath: '/assets/images/frame_1_2.png',
    fallbackWindow: {
      left: 0.05185,
      top: 0.11146,
      width: 0.87963,
      height: 0.49583,
    },
    showBrandingOverlay: false, // Classic has baked-in logos & badge
    badgeCorner: 'RIGHT',
    description: 'The iconic hero poster with classic comic borders.',
  },
  WEB_SHOOTER: {
    id: 'WEB_SHOOTER',
    name: 'Web Shooter',
    assetPath: '/assets/images/frame_readymade.png',
    fallbackWindow: {
      left: 0.13542,
      top: 0.21094,
      width: 0.72396,
      height: 0.42188,
    },
    showBrandingOverlay: true,
    badgeCorner: 'RIGHT',
    description: 'Dynamic web shooter framing with action spider art.',
  },
  SPIDEY_SENSE: {
    id: 'SPIDEY_SENSE',
    name: 'Spidey Sense',
    assetPath: '/assets/images/frame_minispider.png',
    fallbackWindow: {
      left: 0.17188,
      top: 0.23438,
      width: 0.65625,
      height: 0.37793,
    },
    showBrandingOverlay: true,
    badgeCorner: 'RIGHT',
    description: 'Tingle and senses alert frame with hanging mini-spiders.',
  },
  SPIDER_VERSE: {
    id: 'SPIDER_VERSE',
    name: 'Spider-Verse',
    assetPath: '/assets/images/frame_spidergirl.png',
    fallbackWindow: {
      left: 0.24,
      top: 0.24,
      width: 0.52444,
      height: 0.31,
    },
    showBrandingOverlay: true,
    badgeCorner: 'LEFT',
    description: 'Multiverse portal aesthetics with custom left-aligned badge.',
  },
  EVENT_SQUAD: {
    id: 'EVENT_SQUAD',
    name: 'Event Squad',
    assetPath: '/assets/images/frame_event_squad.png',
    fallbackWindow: {
      left: 0.06944,
      top: 0.16113,
      width: 0.86024,
      height: 0.43945,
    },
    showBrandingOverlay: true,
    badgeCorner: 'RIGHT',
    description: 'Wide aperture framed for group and team selfies.',
  },
  SPIDEY_PARTY: {
    id: 'SPIDEY_PARTY',
    name: 'Spidey Party',
    assetPath: '/assets/images/frame_spidey_party.png',
    fallbackWindow: {
      left: 0.08,
      top: 0.18,
      width: 0.84,
      height: 0.58,
    },
    showBrandingOverlay: true,
    badgeCorner: 'RIGHT',
    description: 'Celebratory vibes with party webs and graffiti.',
  },
  GHOST_SPIDER: {
    id: 'GHOST_SPIDER',
    name: 'Ghost Spider',
    assetPath: '/assets/images/frame_ghost_spider.png',
    fallbackWindow: {
      left: 0.08,
      top: 0.16,
      width: 0.84,
      height: 0.6,
    },
    showBrandingOverlay: true,
    badgeCorner: 'RIGHT',
    description: 'Gwen Stacy stylized pink & cyan cyber spider frame.',
  },
};

export const ALL_FILTERS: FilterType[] = [
  'CLASSIC_MASK',
  'WEB_SHOOTER',
  'SPIDEY_SENSE',
  'SPIDER_VERSE',
  'EVENT_SQUAD',
  'SPIDEY_PARTY',
  'GHOST_SPIDER',
];
