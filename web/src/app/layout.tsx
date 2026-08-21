import type { Metadata, Viewport } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'SpideyCode - Encode 5.0 x ZenithCup',
  description:
    'Official Spider-Verse Photobooth & AR Mask Experience for Encode 5.0 x ZenithCup (KIIT, KSAC, IoT Lab, AlgoZenith)',
  applicationName: 'SpideyCode',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'black-translucent',
    title: 'SpideyCode',
  },
  icons: {
    icon: '/assets/logos/encodexzenith_logo.png',
    apple: '/assets/logos/encodexzenith_logo.png',
  },
  manifest: '/manifest.webmanifest',
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  viewportFit: 'cover',
  themeColor: '#0E1320',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full w-full bg-[#0E1320] select-none">
      <head>
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
        <meta name="apple-mobile-web-app-title" content="SpideyCode" />
        <link rel="apple-touch-icon" href="/assets/logos/encodexzenith_logo.png" />
      </head>
      <body className="h-full w-full bg-[#0E1320] text-white antialiased overflow-hidden">
        {children}
        <script
          dangerouslySetInnerHTML={{
            __html: `
              if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
                window.addEventListener('load', function() {
                  navigator.serviceWorker.register('/sw.js').catch(function(err) {
                    console.warn('SW registration failed:', err);
                  });
                });
              }
            `,
          }}
        />
      </body>
    </html>
  );
}
