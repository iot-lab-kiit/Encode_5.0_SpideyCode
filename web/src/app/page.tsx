'use client';

import React, { useState } from 'react';
import { AppScreen, FilterType } from '../types';
import { SplashScreen } from '../components/SplashScreen';
import { GearSelectionScreen } from '../components/GearSelectionScreen';
import { CameraScreen } from '../components/CameraScreen';
import { ReviewScreen } from '../components/ReviewScreen';
import { VideoReviewScreen } from '../components/VideoReviewScreen';
import { IOSInstallPrompt } from '../components/IOSInstallPrompt';

export default function Home() {
  const [currentScreen, setCurrentScreen] = useState<AppScreen>('SPLASH');
  const [selectedFilter, setSelectedFilter] = useState<FilterType>('CLASSIC_MASK');
  const [capturedPosterUrl, setCapturedPosterUrl] = useState<string | null>(null);
  const [capturedVideoBlob, setCapturedVideoBlob] = useState<Blob | null>(null);
  const [capturedVideoUrl, setCapturedVideoUrl] = useState<string | null>(null);

  const handleStart = () => {
    setCurrentScreen('GEAR_SELECTION');
  };

  const handleFilterSelected = (filter: FilterType) => {
    setSelectedFilter(filter);
    setCurrentScreen('CAMERA');
  };

  const handleCameraBack = () => {
    setCurrentScreen('GEAR_SELECTION');
  };

  const handlePhotoCaptured = (filter: FilterType, posterUrl: string) => {
    setSelectedFilter(filter);
    setCapturedPosterUrl(posterUrl);
    setCurrentScreen('REVIEW');
  };

  const handleVideoCaptured = (filter: FilterType, videoBlob: Blob, videoUrl: string) => {
    setSelectedFilter(filter);
    setCapturedVideoBlob(videoBlob);
    setCapturedVideoUrl(videoUrl);
    setCurrentScreen('VIDEO_REVIEW');
  };

  const handleRetake = () => {
    if (capturedVideoUrl) {
      URL.revokeObjectURL(capturedVideoUrl);
      setCapturedVideoUrl(null);
      setCapturedVideoBlob(null);
    }
    setCapturedPosterUrl(null);
    setCurrentScreen('GEAR_SELECTION');
  };

  return (
    <main className="relative h-[100dvh] w-full bg-[#0E1320] flex flex-col justify-center items-center overflow-hidden">
      <IOSInstallPrompt />

      {currentScreen === 'SPLASH' && (
        <SplashScreen onStart={handleStart} />
      )}

      {currentScreen === 'GEAR_SELECTION' && (
        <GearSelectionScreen onSelectFilter={handleFilterSelected} />
      )}

      {currentScreen === 'CAMERA' && (
        <CameraScreen
          initialFilter={selectedFilter}
          onNavigateBack={handleCameraBack}
          onPhotoCaptured={handlePhotoCaptured}
          onVideoCaptured={handleVideoCaptured}
        />
      )}

      {currentScreen === 'REVIEW' && capturedPosterUrl && (
        <ReviewScreen
          filterType={selectedFilter}
          posterDataUrl={capturedPosterUrl}
          onRetake={handleRetake}
        />
      )}

      {currentScreen === 'VIDEO_REVIEW' && capturedVideoBlob && capturedVideoUrl && (
        <VideoReviewScreen
          filterType={selectedFilter}
          videoBlob={capturedVideoBlob}
          videoUrl={capturedVideoUrl}
          onRetake={handleRetake}
        />
      )}
    </main>
  );
}
