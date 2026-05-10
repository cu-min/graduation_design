import { createBrowserRouter } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AdminDashboardPage from '../pages/AdminDashboardPage';
import HomePage from '../pages/HomePage';
import NewsDetailPage from '../pages/NewsDetailPage';
import ProfilePage from '../pages/ProfilePage';
import AdminRouteGuard from './AdminRouteGuard';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'news/:id',
        element: <NewsDetailPage />,
      },
      {
        path: 'profile',
        element: <ProfilePage />,
      },
      {
        path: 'admin',
        element: (
          <AdminRouteGuard>
            <AdminDashboardPage />
          </AdminRouteGuard>
        ),
      },
    ],
  },
]);
