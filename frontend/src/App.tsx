import { Route, Routes } from 'react-router';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AuthPage } from './pages/AuthPage';
import { CatalogPage } from './pages/CatalogPage';
import { CaseDetailsPage } from './pages/CaseDetailsPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { InventoryPage } from './pages/InventoryPage';
import { ProfilePage } from './pages/ProfilePage';
import { StatisticsPage } from './pages/StatisticsPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<CatalogPage category="official" home />} />
        <Route
          path="official"
          element={<CatalogPage key="official" category="official" />}
        />
        <Route
          path="custom"
          element={<CatalogPage key="custom" category="custom" />}
        />
        <Route path="statistics" element={<StatisticsPage />} />
        <Route path="cases/:id" element={<CaseDetailsPage />} />
        <Route path="login" element={<AuthPage key="login" />} />
        <Route path="register" element={<AuthPage key="register" register />} />
        <Route element={<ProtectedRoute />}>
          <Route path="profile" element={<ProfilePage />} />
          <Route path="inventory" element={<InventoryPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
