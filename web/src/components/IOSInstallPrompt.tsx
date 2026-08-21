'use client';

import React, { useEffect, useState } from 'react';
import { Share, PlusSquare, X } from 'lucide-react';

export const IOSInstallPrompt: React.FC = () => {
  const [showPrompt, setShowPrompt] = useState(false);

  useEffect(() => {
    // Check if on iOS
    const isIOS =
      /iPad|iPhone|iPod/.test(navigator.userAgent) && !(window as any).MSStream;
    // Check if already in standalone PWA mode
    const isStandalone =
      (window.navigator as any).standalone === true ||
      window.matchMedia('(display-mode: standalone)').matches;

    const hasDismissed = localStorage.getItem('spidey_ios_pwa_dismissed');

    if (isIOS && !isStandalone && !hasDismissed) {
      setShowPrompt(true);
    }
  }, []);

  if (!showPrompt) return null;

  const handleDismiss = () => {
    localStorage.setItem('spidey_ios_pwa_dismissed', 'true');
    setShowPrompt(false);
  };

  return (
    <div className="fixed top-4 left-4 right-4 z-50 animate-in fade-in slide-in-from-top-4 duration-300">
      <div className="bg-[#161C2C] border-2 border-[#FFDD00] text-white p-3.5 rounded-xl brutalist-shadow flex items-start justify-between gap-3 max-w-md mx-auto">
        <div className="flex-1 text-xs">
          <div className="flex items-center gap-1.5 font-black text-[#FFDD00] text-sm uppercase mb-1">
            <span>🕷️ Install Spidey App on iOS</span>
          </div>
          <p className="text-gray-300 leading-relaxed">
            For full-screen camera & best performance:
          </p>
          <div className="flex items-center gap-2 mt-2 font-bold text-white bg-black/40 p-2 rounded-lg border border-white/10">
            <span>1. Tap Share</span>
            <Share className="w-4 h-4 text-[#00D2FF]" />
            <span>2. Choose &quot;Add to Home Screen&quot;</span>
            <PlusSquare className="w-4 h-4 text-[#FFDD00]" />
          </div>
        </div>
        <button
          onClick={handleDismiss}
          className="p-1 hover:bg-white/10 rounded-full transition-colors text-gray-400 hover:text-white"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
