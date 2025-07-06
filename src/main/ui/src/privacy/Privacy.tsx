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
import { getPrivacyInfo } from "../Api";
import { PrivacyInfo } from "../datatypes/types";
import './privacy.scss';

export const Privacy = (): ReactElement => {

    const [privacyInfo, setPrivacyInfo] = useState<PrivacyInfo | null>(null);

    useEffect(() => {
        const fetchPrivacy = async () : Promise<void> => {
            const res = await getPrivacyInfo();
            if (res !== null) {
                setPrivacyInfo(res);
            }
        };

        fetchPrivacy();
    },[]);

    const renderPrivacy = () : ReactElement => {
        if (privacyInfo === null) {
            return <div className="privacy-content">Loading...</div>;
        }else {
            if (privacyInfo.link !== "") {
                return <a href={privacyInfo.link} target="_blank" rel="noreferrer">Link to Privacy Policy</a>;
            } else if (privacyInfo.html !== "" ) {
                return <div className="privacy-content" dangerouslySetInnerHTML={{__html: privacyInfo.html}} />
            } else {
                return <div className="privacy-no-content">No privacy policy available.</div>;
            }
        }
    }

    return <div className="privacy">
        <h3 className="major">Privacy Policy</h3>
        {renderPrivacy()}
        </div>
}
