'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import Image from 'next/image';
import { FilterType, TransformedFaceData, FlashMode } from '../types';
import { FRAME_DEFINITIONS, ALL_FILTERS } from '../utils/FrameDefinitions';
import { mediaPipeManager } from '../utils/MediaPipeManager';
import { createComposedPoster } from '../utils/ImageCompositionUtils';
import { VideoRecorderUtils } from '../utils/VideoRecorderUtils';
import { FaceOverlayCanvas } from './FaceOverlayCanvas';
import { BrandingOverlay } from './BrandingOverlay';
import { BrutalistBox } from './BrutalistBox';
import {
  ArrowLeft,
  RefreshCw,
  Zap,
  ZapOff,
  Smile,
  AlertTriangle,
} from 'lucide-react';

interface CameraScreenProps {
  initialFilter: FilterType;
  onNavigateBack: () => void;
  onPhotoCaptured: (filter: FilterType, posterDataUrl: string) => void;
  onVideoCaptured: (filter: FilterType, videoBlob: Blob, videoUrl: string) => void;
}

export const CameraScreen: React.FC<CameraScreenProps> = ({
  initialFilter,
  onNavigateBack,
  onPhotoCaptured,
  onVideoCaptured,
}) => {
  const [selectedFilter, setSelectedFilter] = useState<FilterType>(initialFilter);
  const [isFrontCamera, setIsFrontCamera] = useState<boolean>(true);
  const [isMaskEnabled, setIsMaskEnabled] = useState<boolean>(true);
  const [flashMode, setFlashMode] = useState<FlashMode>('OFF');
  const [isCapturing, setIsCapturing] = useState<boolean>(false);
  const [permissionError, setPermissionError] = useState<string | null>(null);

  // Video Recording State
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [recordingProgress, setRecordingProgress] = useState<number>(0);
  const recordingTimerRef = useRef<NodeJS.Timeout | null>(null);
  const videoRecorderRef = useRef<VideoRecorderUtils>(new VideoRecorderUtils());
  const longPressTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const isLongPressRef = useRef<boolean>(false);

  // Screen Flash Effect
  const [showScreenFlash, setShowScreenFlash] = useState<boolean>(false);

  // Media & Face Tracking Refs
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const [viewportSize, setViewportSize] = useState<{ width: number; height: number }>({
    width: 360,
    height: 640,
  });
  const [detectedFaces, setDetectedFaces] = useState<TransformedFaceData[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const animFrameIdRef = useRef<number | null>(null);

  // Stop camera tracks cleanly
  const stopCameraStream = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
  }, []);

  // Initialize camera stream
  const startCamera = useCallback(async () => {
    stopCameraStream();
    setPermissionError(null);

    const facingMode = isFrontCamera ? 'user' : 'environment';
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: facingMode },
          width: { ideal: 1920, min: 640 },
          height: { ideal: 1080, min: 480 },
        },
        audio: true,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play().catch((e) => console.warn('Video play error:', e));
      }
    } catch (err: any) {
      console.error('Camera stream error:', err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setPermissionError(
          'Camera access was denied. Please allow camera permissions in your browser or iOS Safari settings.'
        );
      } else {
        setPermissionError('Could not start camera. Please check your camera device.');
      }
    }
  }, [isFrontCamera, stopCameraStream]);

  // Handle Resize
  useEffect(() => {
    const updateSize = () => {
      if (viewportRef.current) {
        setViewportSize({
          width: viewportRef.current.clientWidth,
          height: viewportRef.current.clientHeight,
        });
      }
    };
    updateSize();
    window.addEventListener('resize', updateSize);
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  // Initialize MediaPipe & Camera
  useEffect(() => {
    mediaPipeManager.initialize();
    startCamera();

    return () => {
      stopCameraStream();
      if (animFrameIdRef.current) {
        cancelAnimationFrame(animFrameIdRef.current);
      }
    };
  }, [startCamera, stopCameraStream]);

  // Real-time Face Tracking Loop
  useEffect(() => {
    let active = true;

    const loop = () => {
      if (!active) return;
      if (videoRef.current && videoRef.current.readyState >= 2) {
        const faces = mediaPipeManager.detectFaces(
          videoRef.current,
          viewportSize.width,
          viewportSize.height,
          isFrontCamera
        );
        setDetectedFaces(faces);
      }
      animFrameIdRef.current = requestAnimationFrame(loop);
    };

    animFrameIdRef.current = requestAnimationFrame(loop);

    return () => {
      active = false;
      if (animFrameIdRef.current) {
        cancelAnimationFrame(animFrameIdRef.current);
      }
    };
  }, [viewportSize, isFrontCamera]);

  // Trigger Shutter / Photo Capture
  const handleCapturePhoto = async () => {
    if (!videoRef.current || isCapturing) return;
    setIsCapturing(true);

    // Screen Flash simulation
    if (flashMode !== 'OFF') {
      setShowScreenFlash(true);
      setTimeout(() => setShowScreenFlash(false), 200);
    }

    try {
      const posterDataUrl = await createComposedPoster(
        videoRef.current,
        selectedFilter,
        isMaskEnabled,
        detectedFaces,
        viewportSize.width,
        viewportSize.height,
        isFrontCamera
      );

      onPhotoCaptured(selectedFilter, posterDataUrl);
    } catch (err) {
      console.error('Capture error:', err);
      alert('Failed to capture poster. Please try again.');
    } finally {
      setIsCapturing(false);
    }
  };

  // Start Video Recording
  const startVideoRecording = () => {
    if (!streamRef.current || isRecording) return;
    const started = videoRecorderRef.current.startRecording(streamRef.current);
    if (!started) return;

    setIsRecording(true);
    setRecordingProgress(0);

    const startTime = Date.now();
    const maxDurationMs = 15000;

    recordingTimerRef.current = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(100, (elapsed / maxDurationMs) * 100);
      setRecordingProgress(progress);

      if (elapsed >= maxDurationMs) {
        stopVideoRecording();
      }
    }, 50);
  };

  // Stop Video Recording
  const stopVideoRecording = async () => {
    if (!isRecording) return;
    if (recordingTimerRef.current) {
      clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }
    setIsRecording(false);
    setRecordingProgress(0);

    const blob = await videoRecorderRef.current.stopRecording();
    if (blob) {
      const url = URL.createObjectURL(blob);
      onVideoCaptured(selectedFilter, blob, url);
    }
  };

  // Shutter Press & Hold Event Handlers (Snapchat-style)
  const handleShutterPointerDown = () => {
    isLongPressRef.current = false;
    longPressTimeoutRef.current = setTimeout(() => {
      isLongPressRef.current = true;
      startVideoRecording();
    }, 450);
  };

  const handleShutterPointerUp = () => {
    if (longPressTimeoutRef.current) {
      clearTimeout(longPressTimeoutRef.current);
      longPressTimeoutRef.current = null;
    }

    if (isRecording) {
      stopVideoRecording();
    } else if (!isLongPressRef.current) {
      handleCapturePhoto();
    }
    isLongPressRef.current = false;
  };

  const activeDef = FRAME_DEFINITIONS[selectedFilter];

  return (
    <div className="relative h-[100dvh] w-full flex flex-col justify-between items-center bg-black text-white overflow-hidden select-none">
      {/* Screen Flash Simulation Layer */}
      {showScreenFlash && (
        <div className="fixed inset-0 z-50 bg-white pointer-events-none animate-out fade-out duration-200" />
      )}

      {/* Permission Denied Notice */}
      {permissionError && (
        <div className="absolute inset-0 z-50 bg-[#0E1320] flex flex-col items-center justify-center p-6 text-center">
          <AlertTriangle className="w-16 h-16 text-[#FFDD00] mb-4" />
          <h2 className="text-xl font-black uppercase text-white mb-2">Camera Permission Needed</h2>
          <p className="text-sm text-gray-300 max-w-sm mb-6 leading-relaxed">
            {permissionError}
          </p>
          <BrutalistBox
            variant="yellow"
            size="md"
            isButton
            onClick={startCamera}
            className="px-6 py-3"
          >
            Retry Camera Access
          </BrutalistBox>
        </div>
      )}

      {/* Top Controls Bar */}
      <div className="relative z-40 w-full flex items-center justify-between px-4 pt-safe py-3 bg-gradient-to-b from-black/80 to-transparent">
        {/* Back Button */}
        <button
          onClick={onNavigateBack}
          className="w-10 h-10 rounded-full bg-[#161C2C]/80 border-2 border-black flex items-center justify-center brutalist-shadow-sm active:scale-95 transition-transform"
        >
          <ArrowLeft className="w-5 h-5 text-white" />
        </button>

        {/* Action Toggles */}
        <div className="flex items-center gap-2.5">
          {/* Flash Mode Toggle */}
          <button
            onClick={() =>
              setFlashMode((prev) => (prev === 'OFF' ? 'ON' : 'OFF'))
            }
            className={`w-10 h-10 rounded-full border-2 border-black flex items-center justify-center brutalist-shadow-sm active:scale-95 transition-all ${
              flashMode === 'ON' ? 'bg-[#FFDD00] text-black' : 'bg-[#161C2C]/80 text-white'
            }`}
          >
            {flashMode === 'ON' ? (
              <Zap className="w-5 h-5 fill-black text-black" />
            ) : (
              <ZapOff className="w-5 h-5" />
            )}
          </button>

          {/* Mask Toggle */}
          <button
            onClick={() => setIsMaskEnabled((prev) => !prev)}
            className={`w-10 h-10 rounded-full border-2 border-black flex items-center justify-center brutalist-shadow-sm active:scale-95 transition-all ${
              isMaskEnabled ? 'bg-[#E50914] text-white' : 'bg-[#161C2C]/80 text-gray-400'
            }`}
          >
            <Smile className="w-5 h-5" />
          </button>

          {/* Flip Camera */}
          <button
            onClick={() => setIsFrontCamera((prev) => !prev)}
            className="w-10 h-10 rounded-full bg-[#161C2C]/80 border-2 border-black flex items-center justify-center brutalist-shadow-sm active:scale-95 transition-transform"
          >
            <RefreshCw className="w-5 h-5 text-white" />
          </button>
        </div>
      </div>

      {/* Main Viewport & AR Canvas Container */}
      <div
        ref={viewportRef}
        className="relative flex-1 w-full max-w-md mx-auto overflow-hidden flex items-center justify-center"
      >
        {/* Live Camera Video Feed */}
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className={`absolute inset-0 w-full h-full object-cover ${
            isFrontCamera ? 'scale-x-[-1]' : ''
          }`}
        />

        {/* Real-time AR Face Mask Canvas */}
        <FaceOverlayCanvas
          faces={detectedFaces}
          isMaskEnabled={isMaskEnabled}
          width={viewportSize.width}
          height={viewportSize.height}
        />

        {/* Selected Frame Graphic Overlay */}
        <div className="absolute inset-0 pointer-events-none z-20">
          <Image
            src={activeDef.assetPath}
            alt={activeDef.name}
            fill
            className="object-contain"
            priority
          />
        </div>

        {/* Dynamic Branding Layer */}
        <BrandingOverlay filterType={selectedFilter} />
      </div>

      {/* Bottom Shutter & Filter Carousel Controls */}
      <div className="relative z-40 w-full flex flex-col items-center pb-safe pt-2 bg-gradient-to-t from-black via-black/80 to-transparent">
        {/* Filter Carousel Switcher */}
        <div className="w-full flex items-center justify-center gap-3 overflow-x-auto no-scrollbar py-2 px-4 max-w-md">
          {ALL_FILTERS.map((filter) => {
            const def = FRAME_DEFINITIONS[filter];
            const isSelected = selectedFilter === filter;
            return (
              <button
                key={filter}
                onClick={() => setSelectedFilter(filter)}
                className={`relative shrink-0 w-12 h-14 rounded-xl overflow-hidden p-0.5 transition-all ${
                  isSelected
                    ? 'border-2 border-[#FFDD00] scale-110 brutalist-shadow-sm'
                    : 'border border-white/20 opacity-60'
                }`}
              >
                <div className="relative w-full h-full rounded-lg overflow-hidden">
                  <Image
                    src={def.assetPath}
                    alt={def.name}
                    fill
                    className="object-cover"
                  />
                </div>
              </button>
            );
          })}
        </div>

        {/* Shutter Button Row */}
        <div className="w-full flex items-center justify-center py-4">
          <div className="relative flex items-center justify-center">
            {/* Circular Video Progress Ring */}
            {isRecording && (
              <svg className="absolute w-24 h-24 -rotate-90 pointer-events-none">
                <circle
                  cx="48"
                  cy="48"
                  r="42"
                  className="stroke-red-600/30"
                  strokeWidth="6"
                  fill="transparent"
                />
                <circle
                  cx="48"
                  cy="48"
                  r="42"
                  className="stroke-[#E50914] transition-all duration-75"
                  strokeWidth="6"
                  strokeDasharray={264}
                  strokeDashoffset={264 - (264 * recordingProgress) / 100}
                  strokeLinecap="round"
                  fill="transparent"
                />
              </svg>
            )}

            {/* Shutter Main Trigger */}
            <button
              onPointerDown={handleShutterPointerDown}
              onPointerUp={handleShutterPointerUp}
              onPointerCancel={handleShutterPointerUp}
              disabled={isCapturing}
              className={`w-20 h-20 rounded-full border-4 border-black brutalist-shadow transition-all transform active:scale-90 flex items-center justify-center ${
                isRecording
                  ? 'bg-[#E50914] scale-95'
                  : 'bg-white hover:bg-gray-100'
              }`}
            >
              <div
                className={`transition-all rounded-full ${
                  isRecording
                    ? 'w-7 h-7 bg-white rounded-md'
                    : 'w-16 h-16 border-2 border-black bg-[#FFDD00] rounded-full'
                }`}
              />
            </button>
          </div>
        </div>

        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">
          Tap for Photo • Hold for Video
        </p>
      </div>
    </div>
  );
};
