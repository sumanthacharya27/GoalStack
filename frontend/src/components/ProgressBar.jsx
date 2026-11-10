export default function ProgressBar({ progress, complete }) {
    return (
        <div
            className="progress gs-progress"
            role="progressbar"
            aria-label="Goal progress"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
        >
            <div
                className={`progress-bar ${complete ? "bg-success" : "gs-bar"}`}
                style={{ width: `${progress}%` }}
            />
        </div>
    );
}
