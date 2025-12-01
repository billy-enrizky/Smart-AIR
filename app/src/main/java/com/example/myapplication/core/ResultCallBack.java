package com.example.myapplication;

public interface ResultCallBack<T> {
    /* this ResultCallBack interface is used for synchronize the progress of the apps and database.
    Otherwise, the asynchronization cause bugs.
    For example, using UserManager.currentUser.firstTime before the reading is done would cause bug.
    Callback make it possible that the code would executed only after the reading is done.
    */

    /*
    /

    /
    The difference between this and the very CallBack interface is:
    we can use it to return value after interacting with database.
    For example(pseudo code),
        //Below is incorrect cuz the user_info before get() is done would be null.
        // yet the database interaction and return statement are done asynchronously.
        // meaning, probably the user_info is null when we return.
        user_info = database.get(some_class);
        return user_info;

        // Below is correct
        user_info = database.get(some_class).addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                user_info = task.some_method();
                ResultCallBack.onComplete(user_info);
            }
        }
        // We override such onComplete when use it, in which we can use the info in user_info.
        // Detailed use of it can be found in userdata.InviteCode.CodeInquiry()
                                  ,along with invitation.ProviderInvitationActivity.LinkToParents()
    */
    void onComplete(T result);
}

