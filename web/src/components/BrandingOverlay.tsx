'use client';

import React from 'react';
import Image from 'next/image';
import { FilterType } from '../types';
import { FRAME_DEFINITIONS, CORNER_LOGOS, EVENT_BADGE_LOGO } from '../utils/FrameDefinitions';

interface BrandingOverlayProps {
  filterType: FilterType;
}

export const BrandingOverlay: React.FC<BrandingOverlayProps> = ({ filterType }) => {
  const frameDef = FRAME_DEFINITIONS[filterType];
  if (!frameDef.showBrandingOverlay) return null;

  const isLeft = frameDef.badgeCorner === 'LEFT';

  return (
    <div className="absolute inset-0 pointer-events-none z-30 flex flex-col justify-between p-3">
      {/* Top Right Corner Society Logos */}
      <div className="w-full flex justify-end items-center gap-1.5">
        {CORNER_LOGOS.map((logo, idx) => (
          <div
            key={idx}
            className="w-8 h-8 rounded-full border-2 border-black overflow-hidden bg-[#161C2C] brutalist-shadow-sm flex items-center justify-center p-0.5"
          >
            <Image
              src={logo}
              alt="Partner Logo"
              width={28}
              height={28}
              className="object-contain rounded-full"
            />
          </div>
        ))}
      </div>

      {/* Bottom Photo Window Corner Event Badge */}
      <div className={`w-full flex ${isLeft ? 'justify-start pl-2' : 'justify-end pr-2'} pb-14`}>
        <div className="relative w-36 h-14 drop-shadow-[0_8px_16px_rgba(0,0,0,0.8)]">
          <Image
            src={EVENT_BADGE_LOGO}
            alt="Event Badge"
            fill
            className="object-contain"
          />
        </div>
      </div>
    </div>
  );
};
