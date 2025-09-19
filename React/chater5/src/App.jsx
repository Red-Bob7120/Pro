import './App.css'
import Header from './components/Header';
import Body from './components/Body';
import Footer from './components/Footer';

function ChildComp(){
  return<div>Child component</div>;
}

function App() {
  return(
  <div className="App">
    <Header />
    <Body>
      <ChildComp />{}
    </Body>
    <Footer />
  </div>
  )
}

export default App
