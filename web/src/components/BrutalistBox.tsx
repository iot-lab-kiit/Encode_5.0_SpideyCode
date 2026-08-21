import React from 'react';

interface BrutalistBoxProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'yellow' | 'red' | 'dark' | 'cyan' | 'white';
  size?: 'sm' | 'md' | 'lg';
  isButton?: boolean;
  disabled?: boolean;
}

export const BrutalistBox: React.FC<BrutalistBoxProps> = ({
  children,
  className = '',
  variant = 'yellow',
  size = 'md',
  isButton = false,
  disabled = false,
  ...props
}) => {
  const variantStyles = {
    yellow: 'bg-[#FFDD00] text-black border-black',
    red: 'bg-[#E50914] text-white border-black',
    dark: 'bg-[#161C2C] text-white border-black',
    cyan: 'bg-[#00D2FF] text-black border-black',
    white: 'bg-white text-black border-black',
  };

  const shadowStyles = {
    sm: 'brutalist-shadow-sm brutalist-border-sm',
    md: 'brutalist-shadow brutalist-border',
    lg: 'brutalist-shadow-lg brutalist-border',
  };

  const buttonActive = isButton && !disabled
    ? 'cursor-pointer active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all duration-75'
    : '';

  const disabledStyle = disabled ? 'opacity-50 cursor-not-allowed' : '';

  return (
    <div
      className={`font-black uppercase tracking-wider ${variantStyles[variant]} ${shadowStyles[size]} ${buttonActive} ${disabledStyle} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};
