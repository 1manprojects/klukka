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
import { DepInfo } from "../../datatypes/types";
import { getDepInfo } from "../../Api";
import './info.scss';

export const Info = (): ReactElement => {

    const [info, setInfo] = useState<DepInfo>({backend: [], frontend: [], version: "error"});


    useEffect(() => {
        const fetchInfo = async (): Promise<void> => {
            const res = await getDepInfo();
            if (res) {
                setInfo(res);
            }
        };
        fetchInfo();
    },[])

    return (
        <div className="info">
            <h1>Klukka - Timetracking</h1>
            <h2>1 Man Projects</h2>
            <h2>Version {info.version}</h2>
            <h3>Github Repository</h3>
            <h3>Licensed under MIT </h3>
            <div className="license-container">
<pre>
Copyright &copy; 2025 Nikolai Reed reed@1manprojects.de   

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the &quot;Software&quot;), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
<br></br>
<br></br>
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
<br></br>
<br></br>
THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
</pre>
            </div>
            <div className="dependencies">
                <h3>Dependencies</h3>
                <span>This Software is built on the following third party dependencies</span>
                <h4>Backend Dependencies</h4>
                <ul>
                    {info.backend.map((dep, index) => (
                        <li key={index}>
                            <span> {dep.name} - {dep.version} <a href={dep.url} target="_blank" rel="noopener noreferrer">{dep.license}</a> </span>
                        </li>
                    ))}
                </ul>
                <h4>Frontend Dependencies</h4>
                <ul>
                    {info.frontend.map((dep, index) => (
                        <li key={index}>
                            <span> {dep.name} - {dep.version} <a href={dep.url} target="_blank" rel="noopener noreferrer">{dep.license}</a> </span>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
