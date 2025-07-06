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
import { Fragment, ReactElement, useEffect, useState } from "react";

import Logo from '../../logo.svg'
import { useLocation } from "react-router";
import { Role } from "../../datatypes/types";
import { getUserRole, sendLogout } from "../../Api";


import './header.scss'
import { faBars } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

export const Header = (): ReactElement => {

    const location = useLocation();
    const [userRole, setUserRole] = useState<Role>("USER");
    const [clazz, setClazz] = useState<string>("normal")

    useEffect(()=> {
        const set = async(): Promise<void> => {
            const res = await getUserRole();
            if (res) {
                setUserRole(res);
            }
        }
        set();
    },[])

    const on_Click = (): void => {
        if (clazz === "normal") {
            setClazz("responsive")
        } else {
            setClazz("normal")
        }
    }

    const onLogout = async (): Promise<void> => {
        await sendLogout();
    }


    return <Fragment><header className="header">
        <a href="myProjects" className="logo">
            <img className="logo" src={Logo} alt="Starting page"/>
        </a>
    
    <div className="menu-grid">
        <div></div>
        <div className={"menu-links " + clazz}>
            <a className={location.pathname === "/myProjects"? "active" : ""} href="myProjects">Projects</a>
            <a className={location.pathname === "/activity"? "active" : ""} href="activity">Activity</a>
            <a className={location.pathname === "/calendar"? "active" : ""} href="calendar">Calendar</a>
            <a className={location.pathname === "/profile"? "active" : ""} href="profile">Profile</a>
            {(userRole === "GROUP" || userRole === "ADMIN") ? <a className={location.pathname === "/groups"? "admin active" : "admin"} href="groups">Manage Groups</a> : null}
            {userRole === "ADMIN" ? <a className={location.pathname === "/admin"? "admin active" : "admin"} href="admin">Administrator</a> : null}
            <a className="logout" onClick={onLogout}>Logout</a>
            <FontAwesomeIcon onClick={on_Click} className="responsive-icon" icon={faBars}/>
        </div>
    </div>
    </header>
    </Fragment>
}
