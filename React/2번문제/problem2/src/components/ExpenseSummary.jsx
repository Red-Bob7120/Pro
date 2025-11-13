import "./ExpenseSummary.css";

const ExpenseSummary = ({ expenses }) => {
  const income = expenses
    .filter((item) => item.type === "income")
    .reduce((sum, item) => sum + item.amount, 0);

  const expense = expenses
    .filter((item) => item.type === "expense")
    .reduce((sum, item) => sum + item.amount, 0);

  const balance = income - expense;

  return (
    <div className="ExpenseSummary">
      <h4 className="summary_title">재정 요약 📊</h4>
      <div className="summary_item income">
        <span className="label">총 수입</span>
        <span className="value">+{income.toLocaleString()}원</span>
      </div>
      <div className="summary_item expense">
        <span className="label">총 지출</span>
        <span className="value">-{expense.toLocaleString()}원</span>
      </div>
      <div className="summary_item balance">
        <span className="label">잔액</span>
        <span className="value">{balance.toLocaleString()}원</span>
      </div>
    </div>
  );
};

export default ExpenseSummary;
