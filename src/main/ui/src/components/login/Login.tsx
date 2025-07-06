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
import { Login as L } from "../../datatypes/types";
import { resetPassword, runValidate, sendLogin } from "../../Api";
import LOGO from '../../resources/Logo.svg';

import './login.scss'
import { useNavigate } from "react-router";

export const Login = (): ReactElement => {

    const [login, setLogin] = useState<L>({mail:"", password:""})
    const [reset, setReset] = useState<boolean>(false);
    const navigate = useNavigate();

    useEffect(() => {
        const onCheck = async () : Promise<void>=> {
                const res: boolean = await runValidate();
                if (!res) {
                    return Promise.reject();
                } else {
                    navigate("/myProjects");
                }
            }
        onCheck();
    },[navigate]);

    const onLogin = async (): Promise<void> => {
        if (reset) {
            const res = await resetPassword(login.mail);
            if (res)
                alert("A reset link has been sent to your mail");
        } else {
            const res = await sendLogin(login);
            if (res !== null) {
                navigate("/myProjects");
            }
        }
    }

    const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>): void => {
        if (e.key === "Enter") {
            onLogin();
        }
    };

    return <div className="login" onKeyDown={handleKeyDown}>
        <div className="logo">
            <img src={LOGO} alt="Klukka Logo" />
        </div>
        <div className="inputs">
            <label>E-mail:</label>
            <input value={login.mail} onChange={(e)=>setLogin({...login, mail:e.target.value})}/>
            {
                !reset? <Fragment>
                <label>Password:</label>
                <input type="password" value={login.password} onChange={(e)=>setLogin({...login, password:e.target.value})}/>
                </Fragment>
                : null}
        </div>
        {!reset? 
        <label className="reset" onClick={()=>setReset(true)}>Reset password</label>
        : null }
        <div className="buttons">
            <button onClick={onLogin}>{reset? "Reset Password" : "Login"}</button>
        </div>
    </div>

}
