import "./ExpenseItem.css";

const ExpenseItem = ({ id, type, description, amount, date, onDelete }) => {
  return (
    <div className={`ExpenseItem ${type}`}>
      <div className="type_col">
        {type === "income" ? "💰 수입" : "💸 지출"}
      </div>
      <div className="description_col">{description}</div>
      <div className="amount_col">
        {type === "income"
          ? `+${amount.toLocaleString()}원`
          : `-${amount.toLocaleString()}원`}
      </div>
      <div className="date_col">{date}</div>
      <div className="btn_col">
        <button onClick={() => onDelete(id)}>삭제</button>
      </div>
    </div>
  );
};

export default ExpenseItem;
