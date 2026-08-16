import './App.css';
import { DocumentPanel } from './components/DocumentPanel';
import { QueryPanel } from './components/QueryPanel';

function App() {
  return (
    <>
      <div className="topbar">
        <div className="topbar__inner">
          <span className="brand-mark">A</span>
          <h1>Anchor</h1>
          <span className="topbar__tagline">Faithfulness Verification</span>
        </div>
      </div>
      <div className="intro">
        <p>
          Add source material, ask a question, and watch each claim in the answer get
          checked against it in real time.
        </p>
      </div>
      <main>
        <DocumentPanel />
        <QueryPanel />
      </main>
    </>
  );
}

export default App;
