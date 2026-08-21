'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { FilterType } from '../types';
import { FRAME_DEFINITIONS, ALL_FILTERS } from '../utils/FrameDefinitions';
import { BrutalistBox } from './BrutalistBox';
import { Sparkles, ArrowRight, CheckCircle2 } from 'lucide-react';

interface GearSelectionScreenProps {
  onSelectFilter: (filter: FilterType) => void;
}

export const GearSelectionScreen: React.FC<GearSelectionScreenProps> = ({
  onSelectFilter,
}) => {
  const [selectedFilter, setSelectedFilter] = useState<FilterType>('CLASSIC_MASK');

  const activeDef = FRAME_DEFINITIONS[selectedFilter];

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center bg-[#0E1320] text-white p-4 overflow-hidden comic-dots-bg pt-safe pb-safe">
      {/* Background Ambience */}
      <div className="absolute inset-0 z-0 opacity-15 pointer-events-none">
        <Image
          src="/assets/images/spider_verse_bg.jpg"
          alt="Ambience"
          fill
          className="object-cover"
        />
      </div>

      {/* Top Header */}
      <div className="relative z-10 w-full flex flex-col items-center pt-2 pb-3">
        <BrutalistBox
          variant="red"
          size="sm"
          className="px-3 py-1 text-[11px] mb-2 inline-flex items-center gap-1.5"
        >
          <Sparkles className="w-3.5 h-3.5" />
          <span>SUIT CUSTOMIZER</span>
        </BrutalistBox>
        <h2 className="text-2xl font-black uppercase tracking-tight text-center">
          EQUIP YOUR <span className="text-[#FFDD00]">FRAME</span>
        </h2>
        <p className="text-xs text-gray-300 font-bold uppercase tracking-wider text-center">
          Swipe & choose your official multiverse photobooth gear
        </p>
      </div>

      {/* Main Frame Showcase & Horizontal Carousel */}
      <div className="relative z-10 w-full flex-1 flex flex-col justify-center items-center my-auto max-w-md">
        {/* Large Frame Preview Card */}
        <div className="relative w-full max-w-[280px] aspect-[9/16] max-h-[420px] bg-[#161C2C] rounded-2xl brutalist-border brutalist-shadow-lg overflow-hidden flex flex-col items-center justify-center p-2 mb-4">
          <div className="relative w-full h-full rounded-xl overflow-hidden bg-black/60 flex items-center justify-center">
            {/* Frame Image Poster */}
            <Image
              src={activeDef.assetPath}
              alt={activeDef.name}
              fill
              className="object-contain"
              priority
            />
            {/* Selected Tag */}
            <div className="absolute top-2 left-2 z-20">
              <span className="bg-[#FFDD00] text-black text-[10px] font-black uppercase px-2 py-0.5 rounded brutalist-shadow-sm flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3 text-black" />
                ACTIVE
              </span>
            </div>
          </div>
        </div>

        {/* Selected Frame Info */}
        <div className="text-center mb-3">
          <h3 className="text-lg font-black uppercase tracking-wide text-[#FFDD00]">
            {activeDef.name}
          </h3>
          <p className="text-xs text-gray-300 font-bold max-w-xs line-clamp-1">
            {activeDef.description}
          </p>
        </div>

        {/* Bottom Horizontal Filter Selector Thumbnails */}
        <div className="w-full flex items-center gap-2.5 overflow-x-auto no-scrollbar py-2 px-1">
          {ALL_FILTERS.map((filter) => {
            const def = FRAME_DEFINITIONS[filter];
            const isSelected = selectedFilter === filter;
            return (
              <button
                key={filter}
                onClick={() => setSelectedFilter(filter)}
                className={`relative shrink-0 w-16 h-20 rounded-xl overflow-hidden p-1 transition-all duration-150 ${
                  isSelected
                    ? 'border-3 border-[#FFDD00] bg-[#FFDD00]/20 scale-105 brutalist-shadow-sm'
                    : 'border-2 border-black bg-[#161C2C] opacity-70 hover:opacity-100'
                }`}
              >
                <div className="relative w-full h-full rounded-lg overflow-hidden">
                  <Image
                    src={def.assetPath}
                    alt={def.name}
                    fill
                    className="object-cover object-center"
                  />
                </div>
                <span className="sr-only">{def.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Action Footer */}
      <div className="relative z-10 w-full max-w-xs mb-2">
        <BrutalistBox
          variant="yellow"
          size="lg"
          isButton
          onClick={() => onSelectFilter(selectedFilter)}
          className="w-full py-3.5 text-center text-base flex items-center justify-center gap-2 hover:bg-[#ffe600]"
        >
          <span>ENTER CAMERA</span>
          <ArrowRight className="w-5 h-5" />
        </BrutalistBox>
      </div>
    </div>
  );
};
