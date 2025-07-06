/*-
 * #%L
 * Klukka
 * %%
 * Copyright (C) 2025 Nikolai Reed reed@1manprojects.de
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
import './App.scss';
import { Login } from './components/login/Login';
import { UserProjects } from './components/user/UserProjects';
import { Fragment } from 'react/jsx-runtime';
import { Header } from './components/header/Header';
import { Admin } from './components/admin/Admin';
import { GroupAdmin } from './components/group/GroupAdmin';
import { CalendarView } from './components/user/Calendar';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { Activity } from './components/user/Activity';
import { Profile } from './components/user/Profile';
import { Reset } from './components/login/Reset';
import { ToastContainer } from 'react-toastify';
import { Info } from './components/info/Info';
import { Footer } from './components/footer/Footer';
import { Privacy } from './privacy/Privacy';

function App(): JSX.Element {

  const router = createBrowserRouter(
    [
      {
        path: "/login",
        element: <Login/>
      },
      {
        path: "/",
        element: <Login/>
      },
      {
        path: "/info",
        element: <Info/>
      },
      {
        path: "/privacy-policy",
        element: <Privacy/>
      },
      {
        path: "/reset/:token",
        element: <Reset/>
      },
      {
        path: "/admin",
        element: <Fragment>
          <Header/>
          <Admin/>
        </Fragment>
      },
      {
        path: "/myProjects",
        element: <Fragment>
          <Header/>
          <UserProjects/>
        </Fragment>
      },
      {
        path:"/activity",
        element: <Fragment>
          <Header/>
          <Activity/>
        </Fragment>
      },
      {
        path:"/groups",
        element: <Fragment>
          <Header/>
          <GroupAdmin/>
        </Fragment>
      },
      {
        path:"/profile",
        element: <Fragment>
          <Header/>
          <Profile/>
        </Fragment>
      },
      {
        path:"/calendar",
        element: <Fragment>
          <Header/>
          <CalendarView/>
        </Fragment>
      }
    ]
  )


  return (
    <div className="App">
      <RouterProvider router={router}/>
      <ToastContainer/>
      <Footer/>
    </div>
  );
}

export default App;
