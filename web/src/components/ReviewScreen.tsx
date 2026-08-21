'use client';

import React, { useEffect } from 'react';
import Image from 'next/image';
import { FilterType } from '../types';
import { FRAME_DEFINITIONS } from '../utils/FrameDefinitions';
import { BrutalistBox } from './BrutalistBox';
import { sharePoster, downloadDataUrl } from '../utils/SharingUtils';
import { ArrowLeft, Download, Share2, RefreshCw } from 'lucide-react';
import confetti from 'canvas-confetti';

interface ReviewScreenProps {
  filterType: FilterType;
  posterDataUrl: string;
  onRetake: () => void;
}

export const ReviewScreen: React.FC<ReviewScreenProps> = ({
  filterType,
  posterDataUrl,
  onRetake,
}) => {
  const frameDef = FRAME_DEFINITIONS[filterType];

  useEffect(() => {
    // Launch celebratory Spidey confetti
    confetti({
      particleCount: 75,
      spread: 70,
      origin: { y: 0.6 },
      colors: ['#E50914', '#FFDD00', '#00D2FF', '#FFFFFF', '#161C2C'],
    });
  }, []);

  const handleDownload = () => {
    const filename = `spidey_code_${frameDef.id.toLowerCase()}_${Date.now()}.jpg`;
    downloadDataUrl(posterDataUrl, filename);
  };

  const handleShare = () => {
    const filename = `spidey_code_${frameDef.id.toLowerCase()}_${Date.now()}.jpg`;
    sharePoster(posterDataUrl, filename);
  };

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center bg-[#0E1320] text-white p-4 overflow-hidden comic-dots-bg pt-safe pb-safe">
      {/* Spider-Verse Background Ambience */}
      <div className="absolute inset-0 z-0 opacity-20 pointer-events-none">
        <Image
          src="/assets/images/spider_verse_bg.jpg"
          alt="Ambience"
          fill
          className="object-cover"
        />
      </div>

      {/* Top Navigation Bar */}
      <div className="relative z-10 w-full flex items-center justify-between py-2 max-w-md">
        <button
          onClick={onRetake}
          className="w-10 h-10 rounded-full bg-[#161C2C] border-2 border-black flex items-center justify-center brutalist-shadow-sm active:scale-95 transition-transform"
        >
          <ArrowLeft className="w-5 h-5 text-white" />
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] font-black uppercase text-[#FFDD00] tracking-widest">
            PHOTO READY
          </span>
          <h2 className="text-lg font-black uppercase tracking-tight text-white">
            {frameDef.name}
          </h2>
        </div>

        <div className="w-10 h-10 opacity-0 pointer-events-none" />
      </div>

      {/* Center High-Resolution Composed Poster Display */}
      <div className="relative z-10 w-full flex-1 flex items-center justify-center my-auto max-w-sm py-2">
        <div className="relative w-full aspect-[9/16] max-h-[500px] rounded-2xl overflow-hidden brutalist-border brutalist-shadow-lg bg-black animate-in zoom-in-95 duration-300">
          <img
            src={posterDataUrl}
            alt="Composed Spidey Poster"
            className="w-full h-full object-contain"
          />
        </div>
      </div>

      {/* Bottom Action Controls (Download, Share, Retake) */}
      <div className="relative z-10 w-full max-w-sm flex flex-col gap-2.5 pb-2">
        <div className="grid grid-cols-2 gap-3">
          {/* Download Button */}
          <BrutalistBox
            variant="yellow"
            size="md"
            isButton
            onClick={handleDownload}
            className="py-3.5 text-center text-sm flex items-center justify-center gap-2 hover:bg-[#ffe600]"
          >
            <Download className="w-4 h-4" />
            <span>SAVE TO PHOTOS</span>
          </BrutalistBox>

          {/* Share Button */}
          <BrutalistBox
            variant="cyan"
            size="md"
            isButton
            onClick={handleShare}
            className="py-3.5 text-center text-sm flex items-center justify-center gap-2 hover:bg-[#33ddff]"
          >
            <Share2 className="w-4 h-4" />
            <span>SHARE SHOT</span>
          </BrutalistBox>
        </div>

        {/* Retake Button */}
        <BrutalistBox
          variant="dark"
          size="sm"
          isButton
          onClick={onRetake}
          className="py-3 text-center text-xs flex items-center justify-center gap-2 hover:bg-[#20283e]"
        >
          <RefreshCw className="w-4 h-4 text-[#FFDD00]" />
          <span>TAKE ANOTHER SHOT</span>
        </BrutalistBox>
      </div>
    </div>
  );
};
