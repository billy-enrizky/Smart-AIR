package com.example.myapplication.SignIn;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.UserData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SignInPresenterTest {

    private SignInView mockView;
    private SignInModel mockModel;
    private SignInPresenter presenter;

    @Before
    public void setUp() {
        mockView = mock(SignInView.class);
        mockModel = mock(SignInModel.class);
        presenter = new SignInPresenter(mockView, mockModel);
        UserManager.currentUser = null;
    }

    @Test
    public void testInitialize() {
        // Test initialize method
        presenter.initialize();
        
        // Verify UserManager.currentUser is set to null
        assertNull(UserManager.currentUser);
        
        // Verify model.ReloadUserAuth() is called
        verify(mockModel).ReloadUserAuth();
    }

    @Test
    public void testSignin_WithNullInput1() {
        // Test signin with null input1
        presenter.signin(null, "password");
        
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithNullInput2() {
        // Test signin with null input2
        presenter.signin("email@test.com", null);
        
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithEmptyInput1() {
        // Test signin with empty input1
        presenter.signin("", "password");
        
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithEmptyInput2() {
        // Test signin with empty input2
        presenter.signin("email@test.com", "");
        
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithEmail() {
        // Test signin with email (should call signInForParentAndProvider)
        String email = "test@example.com";
        String password = "password123";
        
        presenter.signin(email, password);
        
        // Verify SignInAuth is called with email
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        // Verify usernameExists is NOT called
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithUsername() {
        // Test signin with username (should call signInForChild)
        String username = "testuser";
        String password = "password123";
        
        presenter.signin(username, password);
        
        // Verify usernameExists is called
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        // Verify SignInAuth is NOT called
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
    }

    @Test
    public void testSignInForParentAndProvider_Success_FirstTimeTrue() {
        // Test successful signin for parent/provider with FirstTime = true
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        
        // Create a parent account with FirstTime = true
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(true);
        parentAccount.setID(userId);
        
        // Set up mock before calling the method
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        
        // Use a holder to capture the QueryDBforNonChildren callback
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                // Capture the callback argument (index 1)
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        // Call signInForParentAndProvider
        presenter.signInForParentAndProvider(email, password);
        
        // Capture the auth callback
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        // Simulate successful authentication (this will trigger GetCurrentUIDAuth and QueryDBforNonChildren)
        authCallback.getValue().onComplete(true);
        
        // Verify GetCurrentUIDAuth was called
        verify(mockModel).GetCurrentUIDAuth();
        
        // Verify QueryDBforNonChildren was called and callback was captured
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        assertNotNull("QueryDBforNonChildren callback should be captured", queryCallbackHolder[0]);
        
        // Simulate successful query with FirstTime = true
        queryCallbackHolder[0].onComplete(parentAccount);
        
        // Verify welcome message and navigation to onboarding
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToOnBoardingActivity();
        verify(mockView, never()).GoToMainActivity();
    }

    @Test
    public void testSignInForParentAndProvider_Success_FirstTimeFalse() {
        // Test successful signin for parent/provider with FirstTime = false
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        
        // Create a parent account with FirstTime = false
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(false);
        parentAccount.setID(userId);
        
        // Set up mock before calling the method
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        
        // Use a holder to capture the QueryDBforNonChildren callback
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                // Capture the callback argument (index 1)
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        // Call signInForParentAndProvider
        presenter.signInForParentAndProvider(email, password);
        
        // Capture the auth callback
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        // Simulate successful authentication (this will trigger GetCurrentUIDAuth and QueryDBforNonChildren)
        authCallback.getValue().onComplete(true);
        
        // Verify GetCurrentUIDAuth was called
        verify(mockModel).GetCurrentUIDAuth();
        
        // Verify QueryDBforNonChildren was called and callback was captured
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        assertNotNull("QueryDBforNonChildren callback should be captured", queryCallbackHolder[0]);
        
        // Simulate successful query with FirstTime = false
        queryCallbackHolder[0].onComplete(parentAccount);
        
        // Verify welcome message and navigation to main activity
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForParentAndProvider_Failure() {
        // Test failed signin for parent/provider
        String email = "test@example.com";
        String password = "wrongpassword";
        
        // Call signInForParentAndProvider
        presenter.signInForParentAndProvider(email, password);
        
        // Capture the callback
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        // Simulate failed authentication
        authCallback.getValue().onComplete(false);
        
        // Verify error message
        verify(mockView).showShortMessage("User Not Found");
        verify(mockView, never()).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        verify(mockModel, never()).QueryDBforNonChildren(anyString(), any());
    }

    @Test
    public void testSignInForChild_EmptyResult() {
        // Test signin for child when username doesn't exist
        String username = "nonexistent";
        String password = "password123";
        
        // Call signInForChild
        presenter.signInForChild(username, password);
        
        // Capture the callback
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        // Simulate empty result (username not found)
        usernameCallback.getValue().onComplete("");
        
        // Verify error message
        verify(mockView).showShortMessage("User Not Found");
        verify(mockView, never()).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        verify(mockModel, never()).QueryDBforChildren(anyString(), anyString(), any());
    }

    @Test
    public void testSignInForChild_PasswordMismatch() {
        // Test signin for child with wrong password
        String username = "childuser";
        String password = "wrongpassword";
        String parentId = "parent123";
        String correctPassword = "correctpassword";
        
        // Create a child account with different password
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(correctPassword);
        childAccount.setID(username);
        childAccount.setFirstTime(false);
        
        // Call signInForChild
        presenter.signInForChild(username, password);
        
        // Capture the usernameExists callback
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        // Use a holder to capture the QueryDBforChildren callback
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        // Simulate username found
        usernameCallback.getValue().onComplete(parentId);
        
        // Verify QueryDBforChildren was called and callback was captured
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        
        // Simulate successful query but wrong password
        queryCallbackHolder[0].onComplete(childAccount);
        
        // Verify error message
        verify(mockView).showShortMessage("User Not Found");
        verify(mockView, never()).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForChild_Success_FirstTimeTrue() {
        // Test successful signin for child with FirstTime = true
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        
        // Create a child account with FirstTime = true
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        childAccount.setFirstTime(true);
        
        // Call signInForChild
        presenter.signInForChild(username, password);
        
        // Capture the usernameExists callback
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        // Use a holder to capture the QueryDBforChildren callback
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        // Simulate username found
        usernameCallback.getValue().onComplete(parentId);
        
        // Verify QueryDBforChildren was called and callback was captured
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        
        // Simulate successful query with correct password and FirstTime = true
        queryCallbackHolder[0].onComplete(childAccount);
        
        // Verify welcome message and navigation to onboarding
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToOnBoardingActivity();
        verify(mockView, never()).GoToMainActivity();
        assertEquals(childAccount, UserManager.currentUser);
    }

    @Test
    public void testSignInForChild_Success_FirstTimeFalse() {
        // Test successful signin for child with FirstTime = false
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        
        // Create a child account with FirstTime = false
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        childAccount.setFirstTime(false);
        
        // Call signInForChild
        presenter.signInForChild(username, password);
        
        // Capture the usernameExists callback
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        // Use a holder to capture the QueryDBforChildren callback
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        // Simulate username found
        usernameCallback.getValue().onComplete(parentId);
        
        // Verify QueryDBforChildren was called and callback was captured
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        
        // Simulate successful query with correct password and FirstTime = false
        queryCallbackHolder[0].onComplete(childAccount);
        
        // Verify welcome message and navigation to main activity
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        assertEquals(childAccount, UserManager.currentUser);
    }

    @Test
    public void testIsEmail_ValidEmail() {
        // Test isEmail with valid email
        assertTrue(presenter.isEmail("test@example.com"));
        assertTrue(presenter.isEmail("user.name@domain.co.uk"));
        assertTrue(presenter.isEmail("user+tag@example.org"));
    }

    @Test
    public void testIsEmail_InvalidEmail() {
        // Test isEmail with invalid email
        assertFalse(presenter.isEmail("notanemail"));
        assertFalse(presenter.isEmail("test@"));
        assertFalse(presenter.isEmail("@example.com"));
        assertFalse(presenter.isEmail("test@example"));
        assertFalse(presenter.isEmail("username"));
    }

    @Test
    public void testIsNull_NullInput() {
        // Test isNull with null input
        assertTrue(presenter.isNull(null));
    }

    @Test
    public void testIsNull_EmptyString() {
        // Test isNull with empty string
        assertTrue(presenter.isNull(""));
    }

    @Test
    public void testIsNull_NonEmptyString() {
        // Test isNull with non-empty string
        assertFalse(presenter.isNull("not empty"));
        assertFalse(presenter.isNull(" "));
        assertFalse(presenter.isNull("test"));
    }

    @Test
    public void testSignin_WithWhitespaceInputs() {
        // Test signin with whitespace-only inputs (should be trimmed but still valid)
        String email = "  test@example.com  ";
        String password = "  password123  ";
        
        presenter.signin(email, password);
        
        // Verify SignInAuth is called with trimmed values
        verify(mockModel).SignInAuth(eq("test@example.com"), eq("password123"), any());
    }
}

