import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { customCaseArt } from './customCaseArt';

describe('artes das caixas personalizadas', () => {
  it('inclui um WebP local para cada caixa publicada, inclusive as novas', () => {
    const manifest = JSON.parse(readFileSync(new URL('../../../backend/src/main/resources/custom/cases-v1.json', import.meta.url), 'utf8'));
    expect(Object.keys(customCaseArt).sort()).toEqual(manifest.boxes.map((box: { id: string }) => box.id).sort());
    for (const image of Object.values(customCaseArt)) {
      const path = fileURLToPath(new URL(`../../public${image}`, import.meta.url));
      expect(existsSync(path), image).toBe(true);
      const content = readFileSync(path);
      expect(content.toString('ascii', 0, 4)).toBe('RIFF');
      expect(content.toString('ascii', 8, 12)).toBe('WEBP');
    }
  });
});
