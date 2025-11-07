import {useReducer} from "react";

function reducer (state,action){
    switch(action.type){
        case "INCREASE":
            return state + action.data;
        case "DECREASE" :
            return state - action.data;
        case "INIT":
            return 0;
        default:
            return state;
    }
}
function TestComp(){
    const[count,dispatch]=useReducer(reducer,0);    
    //[state 변수, 상태 변화 촐발 함수] = 생성자 (상태변화 함수, 초기값)
    return(
        <div>
         <h4>테스트 컴포넌트</h4>
        <div>
          {count}
        </div>
        <div>
            <button onClick={()=>dispatch({type :"INCREASE",data:1})}>+</button>
            <button onClick={()=>dispatch({type:"INIT"})}>0</button>
            <button onClick={()=>dispatch({type :"DECREASE",data :1})}>-</button>
        </div>


      </div>
    );
}
export default TestComp;