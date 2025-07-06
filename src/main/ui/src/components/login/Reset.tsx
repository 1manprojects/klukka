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
import { ReactElement, useEffect, useState } from "react";

import './login.scss'
import { useParams } from "react-router";
import { checkToken, resetPasswordByToken } from "../../Api";
import LOGO from '../../resources/Logo.svg';

export const Reset = (): ReactElement => {

    const [password, setPassword] = useState<{p1: string, p2: string}>({p1: "", p2: ""});
    const params = useParams();

    const onCheck = async (path: string) : Promise<boolean>=> {
        const res: boolean = await checkToken(path);
        if (!res) {
            window.location.href = "/login";
            return Promise.reject();
        }
        return Promise.resolve(res);
    }

    const onPasswordChange = async (): Promise<void> => {
        if (password.p1 === password.p2 && password.p1.length >= 8) {
            const res = await resetPasswordByToken({token: location.pathname.split("/")[2], newPassword: password.p1});
            if (res) {
                window.location.href = "/login";
            }
        }
    }
    
    useEffect(()=> {
        if (params.token !== undefined) {
            onCheck(params.token);
        }
    },[params]);

    return <div className="login">
        <div className="logo">
            <img src={LOGO} alt="Klukka Logo" />
        </div>
        <div className="inputs">
            <label>New Password:</label>
            <input type="password" value={password.p1} onChange={(e)=>setPassword({...password, p1:e.target.value})}/>
            <label>Repeat Password:</label>
            <input type="password" value={password.p2} onChange={(e)=>setPassword({...password, p2:e.target.value})}/>
            {password.p1 !== password.p2 ? <p className="warning">Passwords do not match</p> : null}
            {password.p1.length < 8 ? <p className="warning">Passwords is to short</p> : null}
        </div>
        <div className="buttons">
            <button disabled={password.p1 !== password.p2} onClick={onPasswordChange}>Reset Password</button>
        </div>
    </div>
}
