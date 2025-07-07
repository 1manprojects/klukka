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
import './footer.scss';
import { getPrivacyInfo, getVersion } from "../../Api";

export const Footer = (): ReactElement => {

    const [version, setVersion] = useState<string>("n/a");
    const [privacyUrl, setPrivacyUrl] = useState<string | null>(null);

    useEffect(() => {
        const fetchVersion = async (): Promise<void> => {
            const res = await getVersion();
            setVersion(res);
            const privacyInfo = await getPrivacyInfo();
            if (privacyInfo !== null && privacyInfo.link !== "") {
                setPrivacyUrl(privacyInfo.link);
            }
        };

        fetchVersion();
    }, []);

    return (
        <footer className="footer">
            <div className="footer-content">
                <a href="https://github.com/1manprojects/klukka" className="footer-link">© 2025 Klukka — MIT License</a>
                <a href="/info" className="footer-link">Version: {version}</a>
                <a href={privacyUrl !== null? privacyUrl : "/privacy-policy"} className="footer-link">Privacy Policy</a>
                <a href="mailto:klukka@1manprojects.de" className="footer-link">Contact</a>
            </div>
        </footer>
    );
}
