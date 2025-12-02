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

    // ==================== initialize() Method Tests ====================
    
    @Test
    public void testInitialize_ResetsCurrentUserToNull() {
        UserManager.currentUser = new ParentAccount();
        presenter.initialize();
        assertNull(UserManager.currentUser);
    }

    @Test
    public void testInitialize_CallsReloadUserAuth() {
        presenter.initialize();
        verify(mockModel).ReloadUserAuth();
    }

    // ==================== signin() Method Tests ====================
    
    @Test
    public void testSignin_WithNullInput1_ShowsErrorMessage() {
        presenter.signin(null, "password");
        verify(mockView).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithNullInput1_DoesNotCallSignInAuth() {
        presenter.signin(null, "password");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
    }

    @Test
    public void testSignin_WithNullInput1_DoesNotCallUsernameExists() {
        presenter.signin(null, "password");
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithNullInput2_ShowsErrorMessage() {
        presenter.signin("email@test.com", null);
        verify(mockView).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithEmptyInput1_ShowsErrorMessage() {
        presenter.signin("", "password");
        verify(mockView).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithEmptyInput2_ShowsErrorMessage() {
        presenter.signin("email@test.com", "");
        verify(mockView).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithEmail_CallsSignInAuth() {
        String email = "test@example.com";
        String password = "password123";
        presenter.signin(email, password);
        verify(mockModel).SignInAuth(eq(email), eq(password), any());
    }

    @Test
    public void testSignin_WithEmail_DoesNotCallUsernameExists() {
        String email = "test@example.com";
        String password = "password123";
        presenter.signin(email, password);
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithUsername_CallsUsernameExists() {
        String username = "testuser";
        String password = "password123";
        presenter.signin(username, password);
        verify(mockModel).usernameExists(eq(username), any());
    }

    @Test
    public void testSignin_WithUsername_DoesNotCallSignInAuth() {
        String username = "testuser";
        String password = "password123";
        presenter.signin(username, password);
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
    }

    @Test
    public void testSignin_WithWhitespaceEmail_TrimsEmail() {
        String email = "  test@example.com  ";
        String password = "password123";
        presenter.signin(email, password);
        verify(mockModel).SignInAuth(eq("test@example.com"), eq(password), any());
    }

    @Test
    public void testSignin_WithWhitespacePassword_TrimsPassword() {
        String email = "test@example.com";
        String password = "  password123  ";
        presenter.signin(email, password);
        verify(mockModel).SignInAuth(eq(email), eq("password123"), any());
    }

    @Test
    public void testSignin_WithWhitespaceUsername_TrimsUsername() {
        String username = "  testuser  ";
        String password = "password123";
        presenter.signin(username, password);
        verify(mockModel).usernameExists(eq("testuser"), any());
    }

    // ==================== signInForParentAndProvider() Method Tests ====================
    
    @Test
    public void testSignInForParentAndProvider_CallsSignInAuth() {
        String email = "test@example.com";
        String password = "password123";
        presenter.signInForParentAndProvider(email, password);
        verify(mockModel).SignInAuth(eq(email), eq(password), any());
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_CallsGetCurrentUIDAuth() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        
        verify(mockModel).GetCurrentUIDAuth();
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_CallsQueryDBforNonChildren() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_ShowsWelcomeMessage() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(false);
        parentAccount.setID(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(mockView).showShortMessage("Welcome!");
    }

    @Test
    public void testSignInForParentAndProvider_FirstTimeTrue_NavigatesToOnboarding() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(true);
        parentAccount.setID(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(mockView).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForParentAndProvider_FirstTimeFalse_NavigatesToMain() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(false);
        parentAccount.setID(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(mockView).GoToMainActivity();
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthFailure_ShowsErrorMessage() {
        String email = "test@example.com";
        String password = "wrongpassword";
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(false);
        verify(mockView).showShortMessage("User Not Found");
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthFailure_DoesNotCallQueryDB() {
        String email = "test@example.com";
        String password = "wrongpassword";
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(false);
        verify(mockModel, never()).QueryDBforNonChildren(anyString(), any());
    }

    // ==================== signInForChild() Method Tests ====================
    
    @Test
    public void testSignInForChild_CallsUsernameExists() {
        String username = "testuser";
        String password = "password123";
        presenter.signInForChild(username, password);
        verify(mockModel).usernameExists(eq(username), any());
    }

    @Test
    public void testSignInForChild_EmptyResult_ShowsErrorMessage() {
        String username = "nonexistent";
        String password = "password123";
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete("");
        verify(mockView).showShortMessage("User Not Found");
    }

    @Test
    public void testSignInForChild_EmptyResult_DoesNotCallQueryDB() {
        String username = "nonexistent";
        String password = "password123";
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete("");
        verify(mockModel, never()).QueryDBforChildren(anyString(), anyString(), any());
    }

    @Test
    public void testSignInForChild_NonEmptyResult_CallsQueryDBforChildren() {
        String username = "childuser";
        String password = "password123";
        String parentId = "parent123";
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
    }

    @Test
    public void testSignInForChild_PasswordMismatch_ShowsErrorMessage() {
        String username = "childuser";
        String password = "wrongpassword";
        String parentId = "parent123";
        String correctPassword = "correctpassword";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(correctPassword);
        childAccount.setID(username);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).showShortMessage("User Not Found");
    }

    @Test
    public void testSignInForChild_Success_SetsCurrentUser() {
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        assertEquals(childAccount, UserManager.currentUser);
    }

    @Test
    public void testSignInForChild_Success_ShowsWelcomeMessage() {
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).showShortMessage("Welcome!");
    }

    @Test
    public void testSignInForChild_FirstTimeTrue_NavigatesToOnboarding() {
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        childAccount.setFirstTime(true);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForChild_FirstTimeFalse_NavigatesToMain() {
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        childAccount.setFirstTime(false);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).GoToMainActivity();
    }

    // ==================== isEmail() Method Tests ====================
    
    @Test
    public void testIsEmail_ValidEmail1_ReturnsTrue() {
        assertTrue(presenter.isEmail("test@example.com"));
    }

    @Test
    public void testIsEmail_ValidEmail2_ReturnsTrue() {
        assertTrue(presenter.isEmail("user.name@domain.co.uk"));
    }

    @Test
    public void testIsEmail_ValidEmail3_ReturnsTrue() {
        assertTrue(presenter.isEmail("user+tag@example.org"));
    }

    @Test
    public void testIsEmail_InvalidEmail1_ReturnsFalse() {
        assertFalse(presenter.isEmail("notanemail"));
    }

    @Test
    public void testIsEmail_InvalidEmail2_ReturnsFalse() {
        assertFalse(presenter.isEmail("test@"));
    }

    @Test
    public void testIsEmail_InvalidEmail3_ReturnsFalse() {
        assertFalse(presenter.isEmail("@example.com"));
    }

    @Test
    public void testIsEmail_InvalidEmail4_ReturnsFalse() {
        assertFalse(presenter.isEmail("test@example"));
    }

    @Test
    public void testIsEmail_InvalidEmail5_ReturnsFalse() {
        assertFalse(presenter.isEmail("username"));
    }

    // ==================== isNull() Method Tests ====================
    
    @Test
    public void testIsNull_NullInput_ReturnsTrue() {
        assertTrue(presenter.isNull(null));
    }

    @Test
    public void testIsNull_EmptyString_ReturnsTrue() {
        assertTrue(presenter.isNull(""));
    }

    @Test
    public void testIsNull_NonEmptyString1_ReturnsFalse() {
        assertFalse(presenter.isNull("not empty"));
    }

    @Test
    public void testIsNull_NonEmptyString2_ReturnsFalse() {
        assertFalse(presenter.isNull(" "));
    }

    @Test
    public void testIsNull_NonEmptyString3_ReturnsFalse() {
        assertFalse(presenter.isNull("test"));
    }
}

