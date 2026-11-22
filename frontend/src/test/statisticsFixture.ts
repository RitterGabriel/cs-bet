import type { PersonalStatistics } from '../services/statistics';
export const statisticsFixture: PersonalStatistics = {
  summary: { balance: '900.00', creditsAdded: '1000.00', walletMovements: 1, availableItems: 2,
    inventoryEstimatedValue: '500.00', unpricedAvailableItems: 0, soldItems: 1, salesTotal: '40.00',
    unvaluedSales: 0, openingHistoryAvailable: false, openedCases: null, openingSpend: null },
  activity: { page: 0, size: 20, totalElements: 3, totalPages: 1, content: [
    { id: 'ITEM_SOLD:1', type: 'ITEM_SOLD', occurredAt: '2026-11-22T12:00:00Z', itemName: 'Fixture skin', amount: '40.00', resultingBalance: null },
    { id: 'ITEM_OBTAINED:1', type: 'ITEM_OBTAINED', occurredAt: '2026-11-22T11:00:00Z', itemName: 'Fixture skin', amount: null, resultingBalance: null },
    { id: 'CREDIT_ADDITION:1', type: 'CREDIT_ADDITION', occurredAt: '2026-11-22T10:00:00Z', itemName: null, amount: '1000.00', resultingBalance: '1000.00' },
  ] },
};
