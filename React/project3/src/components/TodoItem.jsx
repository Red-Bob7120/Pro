import "./TodoItem.css";
import React from "react"

const TodoItem = 
  ({ id, content, isDone, createdDate, onUpdate, onDelete }) => {
  console.log(`${id} 업데이트 확인`)
  const onChangecheckbox =()=>{
    onUpdate(id);
  }
  const onClickDelete =()=>{
    onDelete(id);
  }


  return (
    <div className="TodoItem">
      <div className="checkbox_col">
        <input onChange ={onChangecheckbox}
            checked ={isDone} type="checkbox"/>
      </div>

      <div className="title_col">{content}</div>
      <div className="date_col">
        {new Date(createdDate).toLocaleDateString()}
      </div>

      <div className="btn_col">
        <button onClick={onClickDelete}>삭제</button>
      </div>
    </div>
  );
};

export default React.memo(TodoItem);
