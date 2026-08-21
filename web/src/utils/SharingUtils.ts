export async function downloadDataUrl(dataUrl: string, filename: string) {
  const link = document.createElement('a');
  link.href = dataUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

export async function sharePoster(dataUrl: string, filename: string = 'spidey_code_poster.jpg') {
  try {
    const res = await fetch(dataUrl);
    const blob = await res.blob();
    const file = new File([blob], filename, { type: blob.type });

    if (navigator.canShare && navigator.canShare({ files: [file] })) {
      await navigator.share({
        title: 'SpideyCode - Encode 5.0',
        text: 'Look at my Spider-Verse photobooth shot from Encode 5.0 x ZenithCup! 🕷️⚡',
        files: [file],
      });
      return true;
    } else {
      // Fallback: direct download
      await downloadDataUrl(dataUrl, filename);
      return false;
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      console.warn('Share failed, triggering direct download fallback:', err);
      await downloadDataUrl(dataUrl, filename);
    }
    return false;
  }
}
