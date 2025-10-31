import "./AmountInput.css";
import { useState } from "react";

export default function AmountInput({amount,setAmount}) 
{
    const[amount,setAmount]=useState(1000);
    return(
        <div className="App">
            <h2>
                환전할 금액:
            </h2>
            <h1 className="AmountInput">
                <input
                    type="numbers"
                    value={amount}
                    onChange={(e)=>setAmount(e.target.value)}
                />KRW
            </h1>
            
        </div>

    )
}