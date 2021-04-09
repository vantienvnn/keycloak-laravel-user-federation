/*
   Copyright 2020 Kyriakos Chatzidimitriou

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package info.kyrcha.keycloak.mysqluserfederation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Set;
import org.mindrot.jbcrypt.BCrypt;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

public class MySQLUserStorageProvider
        implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, CredentialInputUpdater {

    protected KeycloakSession session;
    protected Connection conn;
    protected ComponentModel config;

    private static final Logger logger = Logger.getLogger(MySQLUserStorageProvider.class);

    public MySQLUserStorageProvider(KeycloakSession session, ComponentModel config, Connection conn) {
        this.session = session;
        this.config = config;
        this.conn = conn;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UserModel adapter = null;
        String sql = "";
        try {
            sql = "SELECT `" + this.config.getConfig().getFirst("usernamecol") + "`, `"
                    + this.config.getConfig().getFirst("passwordcol") + "` FROM `"
                    + this.config.getConfig().getFirst("table") + "` WHERE `"
                    + this.config.getConfig().getFirst("usernamecol") + "` = ? ;";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1,username);
            rs = stmt.executeQuery();
            String pword = null;
            if (rs.next()) {
                pword = rs.getString(this.config.getConfig().getFirst("passwordcol"));
            }
            if (pword != null) {
                adapter = createAdapter(realm, username);
            }
            // Now do something with the ResultSet ....
        } catch (SQLException ex) {
            // handle any errors
            // System.out.println("SQL: " + sql);
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                stmt = null;
            }
        }
        return adapter;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {
        return new AbstractUserAdapter(session, realm, config) {
            @Override
            public String getUsername() {
                return username;
            }
        };
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        String password = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "";
        try {
            sql = "SELECT `" + this.config.getConfig().getFirst("usernamecol") + "`, `"
                    + this.config.getConfig().getFirst("passwordcol") + "` FROM `"
                    + this.config.getConfig().getFirst("table") + "` WHERE `"
                    + this.config.getConfig().getFirst("usernamecol") + "` = ? ;";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1,user.getUsername());
            rs = stmt.executeQuery();
            if (rs.next()) {
                password = rs.getString(this.config.getConfig().getFirst("passwordcol"));
            }
            // Now do something with the ResultSet ....
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                stmt = null;
            }
        }
        return credentialType.equals(CredentialModel.PASSWORD) && password != null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(CredentialModel.PASSWORD);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()))
            return false;
        String hashPassword = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "";
        try {
            sql = "SELECT `" + this.config.getConfig().getFirst("usernamecol") + "`, `"
                    + this.config.getConfig().getFirst("passwordcol") + "` FROM `"
                    + this.config.getConfig().getFirst("table") + "` WHERE `"
                    + this.config.getConfig().getFirst("usernamecol") + "` = ? ;";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1,user.getUsername());
            rs = stmt.executeQuery();
            if (rs.next()) {
                hashPassword = rs.getString(this.config.getConfig().getFirst("passwordcol"));
            }
            // Now do something with the ResultSet ....
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                stmt = null;
            }
        }

        if (hashPassword == null)
            return false;

        String hashType = this.config.getConfig().getFirst("hash");
        String password = input.getChallengeResponse();

        if (hashType.equalsIgnoreCase("BCrypt")) {
            hashPassword = hashPassword.replaceAll("^\\$2y(.+)$", "\\$2a$1");
            return BCrypt.checkpw(password, hashPassword);
        }

        String hex = null;
        if (hashType.equalsIgnoreCase("SHA1")) {
            hex = DigestUtils.sha1Hex(password);
        } else {
            hex = DigestUtils.md5Hex(password);
        }
        return hashPassword.equalsIgnoreCase(hex);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (input.getType().equals(CredentialModel.PASSWORD)) {
            // @todo continue here for forgot password
            throw new ReadOnlyException("user is read only for this update");
        }
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.EMPTY_SET;
    }

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException sqlEx) {
                logger.error(sqlEx.getMessage());
            } // ignore
            conn = null;
        }
    }

}
