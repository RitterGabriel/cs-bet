export function validateCredentials(
  email: string,
  password: string,
  register = false,
): string | null {
  const normalizedEmail = email.trim();
  if (!normalizedEmail || !password.trim())
    return 'Preencha o e-mail e a senha.';
  if (
    normalizedEmail.length > 150 ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)
  )
    return 'Informe um e-mail válido de até 150 caracteres.';
  if (register && Array.from(password).length < 8)
    return 'A senha deve ter pelo menos 8 caracteres.';
  if (new TextEncoder().encode(password).length > 72)
    return 'A senha deve ter no máximo 72 bytes em UTF-8. Caracteres acentuados podem ocupar mais de um byte.';
  return null;
}

export function safeReturnPath(value: unknown): string {
  return typeof value === 'string' &&
    value.startsWith('/') &&
    !value.startsWith('//') &&
    !value.includes('\\') &&
    !/^\/(login|register)([/?#]|$)/.test(value)
    ? value
    : '/';
}

export const formatCredits = (value: number) =>
  new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
