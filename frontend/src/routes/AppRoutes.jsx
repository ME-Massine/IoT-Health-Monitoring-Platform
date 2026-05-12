import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { DashboardPage } from "../pages/DashboardPage";
import { PatientDetailPage } from "../pages/PatientDetailPage";
import { AlertCenterPage } from "../pages/AlertCenterPage";
import { DevicesPage } from "../pages/DevicesPage";
import { NotFoundPage } from "../pages/NotFoundPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    errorElement: <NotFoundPage />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: "patients/:patientId",
        element: <PatientDetailPage />,
      },
      {
        path: "alerts",
        element: <AlertCenterPage />,
      },
      {
        path: "devices",
        element: <DevicesPage />,
      },
    ],
  },
]);

export function AppRoutes() {
  return <RouterProvider router={router} />;
}