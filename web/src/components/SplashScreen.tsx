'use client';

import React, { useEffect, useState, useRef } from 'react';
import Image from 'next/image';
import { BrutalistBox } from './BrutalistBox';
import { Sparkles, Camera } from 'lucide-react';
import lottie from 'lottie-web';

interface SplashScreenProps {
  onStart: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onStart }) => {
  const lottieContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let animInstance: any = null;

    fetch('/assets/animations/spider_walk.json')
      .then((res) => res.json())
      .then((animationData) => {
        if (lottieContainerRef.current) {
          animInstance = lottie.loadAnimation({
            container: lottieContainerRef.current,
            renderer: 'svg',
            loop: true,
            autoplay: true,
            animationData,
          });
        }
      })
      .catch((e) => console.warn('Could not load Lottie animation:', e));

    return () => {
      if (animInstance) {
        animInstance.destroy();
      }
    };
  }, []);

  return (
    <div className="relative min-h-[100dvh] w-full flex flex-col justify-between items-center bg-[#0E1320] text-white p-6 overflow-hidden comic-dots-bg pt-safe pb-safe">
      {/* Background Graphic with Vignette */}
      <div className="absolute inset-0 z-0 opacity-20 pointer-events-none">
        <Image
          src="/assets/images/spider_verse_bg.jpg"
          alt="Spider-Verse Background"
          fill
          className="object-cover object-center"
          priority
        />
      </div>

      {/* Top Organization Logos */}
      <div className="relative z-10 w-full flex justify-center items-center gap-4 py-3">
        <div className="flex items-center gap-3 bg-[#161C2C]/90 px-4 py-2 rounded-full border-2 border-black brutalist-shadow-sm backdrop-blur-sm">
          <Image
            src="/assets/logos/algozenith_logo.png"
            alt="AlgoZenith"
            width={32}
            height={32}
            className="rounded-full"
          />
          <Image
            src="/assets/logos/iot_logo.png"
            alt="IoT Lab"
            width={32}
            height={32}
            className="rounded-full"
          />
          <Image
            src="/assets/logos/ksac_logo.webp"
            alt="KSAC"
            width={32}
            height={32}
            className="rounded-full"
          />
        </div>
      </div>

      {/* Hero Content Section */}
      <div className="relative z-10 flex flex-col items-center text-center max-w-sm my-auto">
        {/* Animated Spider Container */}
        <div
          ref={lottieContainerRef}
          className="w-28 h-28 -mb-4 drop-shadow-[0_10px_20px_rgba(229,9,20,0.5)] flex items-center justify-center"
        />

        {/* Main Event Badge */}
        <div className="relative mb-6 drop-shadow-[0_12px_24px_rgba(0,0,0,0.8)]">
          <Image
            src="/assets/logos/encodexzenith_logo.png"
            alt="Encode 5.0 x ZenithCup"
            width={300}
            height={120}
            className="object-contain"
            priority
          />
        </div>

        {/* Title Tag */}
        <BrutalistBox
          variant="red"
          size="sm"
          className="px-3 py-1 text-xs mb-3 rotate-[-1deg] inline-flex items-center gap-1.5"
        >
          <Sparkles className="w-3.5 h-3.5" />
          <span>OFFICIAL AR PHOTOBOOTH</span>
        </BrutalistBox>

        <h1 className="text-3xl font-black uppercase tracking-tight text-white drop-shadow-md">
          SPIDEY<span className="text-[#FFDD00]">CODE</span>
        </h1>
        <p className="text-xs text-gray-300 font-bold uppercase tracking-wider mt-1 max-w-[260px]">
          Suit up with comic AR masks & exclusive event frames
        </p>
      </div>

      {/* Bottom CTA Action Button */}
      <div className="relative z-10 w-full max-w-xs mb-4">
        <BrutalistBox
          variant="yellow"
          size="lg"
          isButton
          onClick={onStart}
          className="w-full py-4 text-center text-lg flex items-center justify-center gap-2 hover:bg-[#ffe600]"
        >
          <Camera className="w-5 h-5" />
          <span>ACTIVATE GEAR</span>
        </BrutalistBox>
        <p className="text-center text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">
          Designed for iOS & Cross-Platform Web
        </p>
      </div>
    </div>
  );
};