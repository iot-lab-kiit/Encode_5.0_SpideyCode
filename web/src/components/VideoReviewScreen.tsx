'use client';

import React, { useRef } from 'react';
import Image from 'next/image';
import { FilterType } from '../types';
import { FRAME_DEFINITIONS } from '../utils/FrameDefinitions';
import { BrandingOverlay } from './BrandingOverlay';
import { BrutalistBox } from './BrutalistBox';
import { ArrowLeft, Download, Share2, RefreshCw, Video } from 'lucide-react';

interface VideoReviewScreenProps {
  filterType: FilterType;
  videoBlob: Blob;
  videoUrl: string;
  onRetake: () => void;
}

export const VideoReviewScreen: React.FC<VideoReviewScreenProps> = ({
  filterType,
  videoBlob,
  videoUrl,
  onRetake,
}) => {
  const frameDef = FRAME_DEFINITIONS[filterType];
  const videoRef = useRef<HTMLVideoElement | null>(null);

  const handleDownload = () => {
    const filename = `spidey_clip_${frameDef.id.toLowerCase()}_${Date.now()}.mp4`;
    const link = document.createElement('a');
    link.href = videoUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleShare = async () => {
    const filename = `spidey_clip_${frameDef.id.toLowerCase()}_${Date.now()}.mp4`;
    try {
      const file = new File([videoBlob], filename, { type: videoBlob.type || 'video/mp4' });
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({
          title: 'SpideyCode Video - Encode 5.0',
          text: 'Check out my Spider-Verse video from Encode 5.0 x ZenithCup! 🕷️🎥',
          files: [file],
        });
      } else {
        handleDownload();
      }
    } catch (e: any) {
      if (e.name !== 'AbortError') {
        handleDownload();
      }
    }
  };

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center bg-[#0E1320] text-white p-4 overflow-hidden comic-dots-bg pt-safe pb-safe">
      {/* Ambience */}
      <div className="absolute inset-0 z-0 opacity-20 pointer-events-none">
        <Image
          src="/assets/images/spider_verse_bg.jpg"
          alt="Ambience"
          fill
          className="object-cover"
        />
      </div>

      {/* Top Bar */}
      <div className="relative z-10 w-full flex items-center justify-between py-2 max-w-md">
        <button
          onClick={onRetake}
          className="w-10 h-10 rounded-full bg-[#161C2C] border-2 border-black flex items-center justify-center brutalist-shadow-sm active:scale-95 transition-transform"
        >
          <ArrowLeft className="w-5 h-5 text-white" />
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] font-black uppercase text-[#E50914] tracking-widest flex items-center gap-1">
            <Video className="w-3 h-3" />
            VIDEO READY
          </span>
          <h2 className="text-lg font-black uppercase tracking-tight text-white">
            {frameDef.name}
          </h2>
        </div>

        <div className="w-10 h-10 opacity-0 pointer-events-none" />
      </div>

      {/* Center Video Player with Frame Overlay */}
      <div className="relative z-10 w-full flex-1 flex items-center justify-center my-auto max-w-sm py-2">
        <div className="relative w-full aspect-[9/16] max-h-[500px] rounded-2xl overflow-hidden brutalist-border brutalist-shadow-lg bg-black">
          {/* Playing Video Track */}
          <video
            ref={videoRef}
            src={videoUrl}
            autoPlay
            loop
            playsInline
            controls
            className="w-full h-full object-cover"
          />

          {/* Frame Graphic Overlay */}
          <div className="absolute inset-0 pointer-events-none z-20">
            <Image
              src={frameDef.assetPath}
              alt={frameDef.name}
              fill
              className="object-contain"
              priority
            />
          </div>

          {/* Dynamic Branding Layer */}
          <BrandingOverlay filterType={filterType} />
        </div>
      </div>

      {/* Bottom Action Controls */}
      <div className="relative z-10 w-full max-w-sm flex flex-col gap-2.5 pb-2">
        <div className="grid grid-cols-2 gap-3">
          <BrutalistBox
            variant="yellow"
            size="md"
            isButton
            onClick={handleDownload}
            className="py-3.5 text-center text-sm flex items-center justify-center gap-2 hover:bg-[#ffe600]"
          >
            <Download className="w-4 h-4" />
            <span>SAVE CLIP</span>
          </BrutalistBox>

          <BrutalistBox
            variant="cyan"
            size="md"
            isButton
            onClick={handleShare}
            className="py-3.5 text-center text-sm flex items-center justify-center gap-2 hover:bg-[#33ddff]"
          >
            <Share2 className="w-4 h-4" />
            <span>SHARE CLIP</span>
          </BrutalistBox>
        </div>

        <BrutalistBox
          variant="dark"
          size="sm"
          isButton
          onClick={onRetake}
          className="py-3 text-center text-xs flex items-center justify-center gap-2 hover:bg-[#20283e]"
        >
          <RefreshCw className="w-4 h-4 text-[#FFDD00]" />
          <span>RECORD ANOTHER</span>
        </BrutalistBox>
      </div>
    </div>
  );
};