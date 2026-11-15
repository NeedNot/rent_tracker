import { cronJobs } from "convex/server";
import { internal } from "./_generated/api";

const crons = cronJobs();

crons.hourly("late rent pushes", { minuteUTC: 0 }, internal.notifications.dispatchHourly);

export default crons;
