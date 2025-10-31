import { useState } from 'react'
import Viewer from './components/Viewer'
import './App.css'
import Controller from './components/Controller'
import { use } from 'react';

function App() {
const [count, setCount] = useState(0);
const [ text, setText]= useState("");

const handleSetCount = (value) =>{
    setCount(count+value);
  };
const handleChangeText =(e) =>{
    setText(e.target.value);
  };
useEffect(()=>{
  console.log("Count 업데이트 :", text,count);
},[count]);
const handleRset = (value) =>{
    setCount(0);
}

  
  
  return<div className="App">
    <h1>Simple Conunter</h1>
    <Section>
      <input value = {text} onChange ={handleChangeText}/>
    </Section>
    <section>
      <Viewer count ={count}/>
    </section>
    <section>
      <Controller HandleSetCount ={handleSetCount}
      />
    </section>
  </div>
  
};


export default App
