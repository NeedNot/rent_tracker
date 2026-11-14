create table "signInCodeRequest" ("email" text not null, "requestedAt" integer not null);

create index "signInCodeRequest_email_requestedAt_idx" on "signInCodeRequest" ("email", "requestedAt");

create index "signInCodeRequest_requestedAt_idx" on "signInCodeRequest" ("requestedAt");
