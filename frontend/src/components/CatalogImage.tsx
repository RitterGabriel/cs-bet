import { useState } from 'react';

// Exibe as URLs da fonte sem copiar as imagens para o projeto.
// A opção false permite desativar imagens remotas quando necessário.
const remoteEnabled = import.meta.env.VITE_CATALOG_IMAGES_ENABLED !== 'false';
export function permittedImage(src: string, remote: boolean): boolean {
  if (!remote) return src.startsWith('/images/');
  if (!remoteEnabled) return false;
  try {
    const url = new URL(src);
    return (
      url.protocol === 'https:' &&
      !url.username &&
      !url.password &&
      [
        'community.akamai.steamstatic.com',
        'cdn.steamstatic.com',
        'raw.githubusercontent.com',
      ].includes(url.hostname)
    );
  } catch {
    return false;
  }
}
export function CatalogImage({
  src,
  alt,
  remote = false,
  item = false,
  loading = 'lazy',
}: {
  src: string;
  alt: string;
  remote?: boolean;
  item?: boolean;
  loading?: 'lazy' | 'eager';
}) {
  const [failed, setFailed] = useState('');
  const fallback = item
    ? '/images/catalog-item.svg'
    : '/images/catalog-case.svg';
  const actual = permittedImage(src, remote) && failed !== src ? src : fallback;
  return (
    <img
      src={actual}
      alt={actual === fallback ? `Imagem ilustrativa de reserva: ${alt}` : alt}
      width="640"
      height="420"
      loading={loading}
      referrerPolicy="no-referrer"
      onError={() => {
        if (actual !== fallback) setFailed(src);
      }}
    />
  );
}
