package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.UserInfo;
import org.geogig.web.model.UserInfoPrivateProfile;

public class User extends Identity<UserInfo> {

    private Optional<BufferedImage> avatar;

    User(Client client, UserInfo info) {
        super(client, info, () -> info.getId(), () -> info.getIdentity());
    }

    public Client getClient() {
        return super.client;
    }

    // modify this user, having changed its UserInfo
    public User modify() {
        UserInfo userInfo = getInfo();
        UsersClient api = getClient().users();
        User modified = api.modify(userInfo);
        super.updateInfo(modified.getInfo());
        return this;
    }

    public Optional<BufferedImage> getAvatar() {
        if (avatar == null) {
            avatar = getClient().users().getAvatarByUser(getIdentity());
        }
        return avatar;
    }

    public List<Repo> getRepos() {
        List<RepositoryInfo> userRepoInfos;
        try {
            userRepoInfos = client.users.getUserRepositories(getIdentity());
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return new ArrayList<>();
        }
        return userRepoInfos.stream().map((r) -> new Repo(client, r)).collect(Collectors.toList());
    }

    public Repo getRepo(String repoName) {
        checkNotNull(repoName);
        return client.repositories().getRepo(getIdentity(), repoName);
    }

    public void setPassword(String newPassword) {
        try {
            client.users.resetPassword(getIdentity(), newPassword);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> getFollowers() {
        throw new UnsupportedOperationException();
    }

    public List<User> getFollowing() {
        throw new UnsupportedOperationException();
    }

    public boolean follow(String userName) {
        throw new UnsupportedOperationException();
    }

    public boolean unfollow(String userName) {
        throw new UnsupportedOperationException();
    }

    public Repo createRepo(String repoName) {
        Repo repo = client.repositories().create(getIdentity(), repoName);
        return repo;
    }

    public ReposClient repositories() {
        return client.repositories();
    }

    public String getLoginName() {
        return getInfo().getIdentity();
    }

    public @Nullable String getFullName() {
        UserInfoPrivateProfile pf = getInfo().getPrivateProfile();
        return pf == null ? null : pf.getFullName();
    }

    public void setFullName(String fullName) {
        UserInfoPrivateProfile pf = getInfo().getPrivateProfile();
        if (pf == null) {
            pf = new UserInfoPrivateProfile();
            getInfo().setPrivateProfile(pf);
        }
        pf.setFullName(fullName);
    }

    public boolean isAdmin() {
        return getInfo().isSiteAdmin();
    }
}
