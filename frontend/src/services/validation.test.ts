import { describe, expect, it } from 'vitest';
import {
  formatCredits,
  safeReturnPath,
  validateCredentials,
} from './validation';

describe('validação alinhada ao backend', () => {
  it('aceita e-mail e senha válidos', () => {
    expect(
      validateCredentials(' player@example.com ', 'strong-password', true),
    ).toBeNull();
  });
  it('não impõe mínimo de cadastro ao login', () => {
    expect(validateCredentials('player@example.com', 'short')).toBeNull();
    expect(validateCredentials('player@example.com', 'short', true)).toContain(
      '8 caracteres',
    );
  });
  it('exige e-mail e senha e rejeita e-mail inválido', () => {
    expect(validateCredentials('', 'password')).not.toBeNull();
    expect(validateCredentials('player@example.com', '   ')).not.toBeNull();
    expect(validateCredentials('invalid', 'password')).not.toBeNull();
  });
  it('respeita os 72 bytes de BCrypt para UTF-8', () => {
    expect(
      validateCredentials('p@example.com', 'é'.repeat(36), true),
    ).toBeNull();
    expect(
      validateCredentials('p@example.com', 'é'.repeat(37), true),
    ).toContain('72 bytes');
  });
  it('restringe retorno após login a caminhos internos', () => {
    for (const path of [
      '//example.com',
      'https://example.com',
      '/\\example.com',
      '/login',
      '/register?x=y',
      undefined,
    ]) {
      expect(safeReturnPath(path)).toBe('/');
    }
    expect(safeReturnPath('/profile')).toBe('/profile');
  });
  it('formata créditos sem símbolo monetário', () => {
    expect(formatCredits(1234.5)).toBe('1.234,50');
  });
});
