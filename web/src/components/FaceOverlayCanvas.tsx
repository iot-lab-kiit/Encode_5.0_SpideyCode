'use client';

import React, { useEffect, useRef } from 'react';
import { TransformedFaceData } from '../types';
import { SPIDEY_MASK_SRC } from '../utils/FrameDefinitions';
import { SPIDEY_MASK_REFERENCE } from '../utils/MediaPipeManager';

interface FaceOverlayCanvasProps {
  faces: TransformedFaceData[];
  isMaskEnabled: boolean;
  width: number;
  height: number;
}

export const FaceOverlayCanvas: React.FC<FaceOverlayCanvasProps> = ({
  faces,
  isMaskEnabled,
  width,
  height,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const maskImgRef = useRef<HTMLImageElement | null>(null);

  useEffect(() => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.src = SPIDEY_MASK_SRC;
    img.onload = () => {
      maskImgRef.current = img;
    };
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, width, height);

    if (!isMaskEnabled || !maskImgRef.current || faces.length === 0) {
      return;
    }

    const maskImg = maskImgRef.current;

    for (const face of faces) {
      if (face.leftEye && face.rightEye) {
        const eyeCenterX = (face.leftEye.x + face.rightEye.x) / 2;
        const eyeCenterY = (face.leftEye.y + face.rightEye.y) / 2;

        const faceWidth = face.boundingBox.width;
        const maskScale =
          faceWidth / SPIDEY_MASK_REFERENCE.referenceMaskFaceWidth;
        const deltaAngleRad =
          ((face.eyeAngleDeg - SPIDEY_MASK_REFERENCE.maskEyeAngleDeg) * Math.PI) /
          180;

        ctx.save();
        ctx.translate(eyeCenterX, eyeCenterY);
        ctx.rotate(deltaAngleRad);
        ctx.scale(maskScale, maskScale);

        ctx.drawImage(
          maskImg,
          -SPIDEY_MASK_REFERENCE.eyeCenter.x,
          -SPIDEY_MASK_REFERENCE.eyeCenter.y
        );
        ctx.restore();
      }
    }
  }, [faces, isMaskEnabled, width, height]);

  return (
    <canvas
      ref={canvasRef}
      width={width}
      height={height}
      className="absolute inset-0 pointer-events-none z-20 w-full h-full"
    />
  );
};
