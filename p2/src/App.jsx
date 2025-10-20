import { useState } from "react";
import { useEffect, useRef } from "react";
import Viewer from "./components/Viewer";
import Controller from "./components/Controller";
import { use } from "react";
import Even from "./components/Even";

function App() {
  const [count, setCount] = useState(0);

  // count 값 변경
  const handleSetCount = (value) => {
    setCount(count + value);
  };

  // 초기화
  const handleReset = () => {
    setCount(0);
  };
  const didMountRef = useRef(false);

  useEffect(()=>{
    if(!didMountRef.current){
      didMountRef.current = true;
      return;
    }else{
      console.log("컴포번트 업데이트!")
    }
  });
  useEffect(()=>{
    console.log("컴포넌트 마운트");
}, []);
useEffect(()=>{
 const intervalID = setInterval(()=>{
    console.log("깜빡");
  },  1000);
  return()=>{
    console.log("클린업");
    clearInterval(intervalID);
  }
});
  return (
    <div className="App">
      <h1>Simple Counter</h1>
        {count % 2 === 0 &&<Even />}
      <section>
        <Viewer count={count} />
      </section>
     <section>
      <Controller handleSetCount={handleSetCount} handleReset={handleReset} />
     </section>
    </div>
  );
}

export default App;
