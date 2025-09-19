import React from 'react';
import{useState} from "react"; 

function Body(){
    const[text,setText] = useState("");

    const handleOnchage=(e)=>{
        setText(e.target.value);
    };
    
    const handleOnClick = () =>{
        alert(text);
    }
    return (
        <div>
            <input value ={text} onChange ={handleOnchage}/>
            <button onClick={handleOnClick}>작성 완료</button>
        </div>
    );
}
export default Body;