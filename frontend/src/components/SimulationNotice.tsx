export const SIMULATION_NOTICE =
  'O CS Bet é uma plataforma de simulação. Todos os saldos e itens são virtuais, sem valor monetário. Nenhum pagamento é necessário e não é possível sacar ou converter valores em dinheiro.';
export function SimulationNotice() {
  return <p className="simulation-notice">{SIMULATION_NOTICE}</p>;
}
