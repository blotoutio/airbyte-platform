import { faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { load, YAMLException } from "js-yaml";
import isEqual from "lodash/isEqual";
import lowerCase from "lodash/lowerCase";
import startCase from "lodash/startCase";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { FormattedMessage } from "react-intl";
import { useNavigate } from "react-router-dom";

import { HeadTitle } from "components/common/HeadTitle";
import {
  BuilderFormValues,
  DEFAULT_BUILDER_FORM_VALUES,
  DEFAULT_CONNECTOR_NAME,
  DEFAULT_JSON_MANIFEST_VALUES,
} from "components/connectorBuilder/types";
import { useManifestToBuilderForm } from "components/connectorBuilder/useManifestToBuilderForm";
import { Button, ButtonProps } from "components/ui/Button";
import { Card } from "components/ui/Card";
import { FlexContainer } from "components/ui/Flex";
import { Heading } from "components/ui/Heading";
import { Text } from "components/ui/Text";
import { ToastType } from "components/ui/Toast";

import { Action, Namespace } from "core/analytics";
import { ConnectorManifest, DeclarativeComponentSchema } from "core/request/ConnectorManifest";
import { useAnalyticsService } from "hooks/services/Analytics";
import { useNotificationService } from "hooks/services/Notification";
import {
  ConnectorBuilderLocalStorageProvider,
  useConnectorBuilderLocalStorage,
} from "services/connectorBuilder/ConnectorBuilderLocalStorageService";
import { useCreateProject } from "services/connectorBuilder/ConnectorBuilderProjectsService";

import { ReactComponent as AirbyteLogo } from "./airbyte-logo.svg";
import styles from "./ConnectorBuilderCreatePage.module.scss";
import { ReactComponent as ImportYamlImage } from "./import-yaml.svg";
import { ReactComponent as StartFromScratchImage } from "./start-from-scratch.svg";
import { getEditPath } from "../ConnectorBuilderRoutes";

const YAML_UPLOAD_ERROR_ID = "connectorBuilder.yamlUpload.error";
const CREATE_PROJECT_ERROR_ID = "connectorBuilder.createProject.error";

const ConnectorBuilderCreatePageInner: React.FC = () => {
  const analyticsService = useAnalyticsService();
  const { mutateAsync: createProject, isLoading: isCreateProjectLoading } = useCreateProject();
  const [activeTile, setActiveTile] = useState<"yaml" | "empty" | undefined>();
  const { storedFormValues, setStoredFormValues, storedManifest, setStoredManifest, setStoredEditorView } =
    useConnectorBuilderLocalStorage();
  const navigate = useNavigate();

  // use refs for the intial values because useLocalStorage changes the references on re-render
  const initialStoredFormValues = useRef<BuilderFormValues>(storedFormValues);
  const initialStoredManifest = useRef<ConnectorManifest>(storedManifest);

  useEffect(() => {
    if (
      !isEqual(initialStoredFormValues.current, DEFAULT_BUILDER_FORM_VALUES) ||
      !isEqual(initialStoredManifest.current, DEFAULT_JSON_MANIFEST_VALUES)
    ) {
      navigate(`../${getEditPath("1234")}`, { replace: true });
    }
  }, [navigate]);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const { registerNotification, unregisterNotificationById } = useNotificationService();
  const { convertToBuilderFormValues } = useManifestToBuilderForm();
  const [importYamlLoading, setImportYamlLoading] = useState(false);

  useEffect(() => {
    analyticsService.track(Namespace.CONNECTOR_BUILDER, Action.CONNECTOR_BUILDER_START, {
      actionDescription: "Connector Builder UI create page opened",
    });
  }, [analyticsService]);

  const createAndNavigate = useCallback(
    async (name: string, manifest?: DeclarativeComponentSchema) => {
      try {
        const result = await createProject({ name, manifest });
        navigate(`../${getEditPath(result.builderProjectId)}`);
      } catch (e) {
        registerNotification({
          id: CREATE_PROJECT_ERROR_ID,
          text: (
            <FormattedMessage
              id={CREATE_PROJECT_ERROR_ID}
              values={{
                reason: e,
              }}
            />
          ),
          type: ToastType.ERROR,
        });
      }
    },
    [createProject, navigate, registerNotification]
  );

  const handleYamlUpload = useCallback(
    async (uploadEvent: React.ChangeEvent<HTMLInputElement>) => {
      setImportYamlLoading(true);
      const file = uploadEvent.target.files?.[0];
      const reader = new FileReader();
      reader.onload = async (readerEvent) => {
        const yaml = readerEvent.target?.result as string;
        const fileName = file?.name;

        try {
          let json;
          try {
            json = load(yaml) as ConnectorManifest;
          } catch (e) {
            if (e instanceof YAMLException) {
              registerNotification({
                id: YAML_UPLOAD_ERROR_ID,
                text: (
                  <FormattedMessage
                    id={YAML_UPLOAD_ERROR_ID}
                    values={{
                      reason: e.reason,
                      line: e.mark.line,
                    }}
                  />
                ),
                type: ToastType.ERROR,
              });
              analyticsService.track(Namespace.CONNECTOR_BUILDER, Action.INVALID_YAML_UPLOADED, {
                actionDescription: "A file with invalid YAML syntax was uploaded to the Connector Builder create page",
                error_message: e.reason,
              });
            }
            return;
          }

          let convertedFormValues;
          try {
            convertedFormValues = await convertToBuilderFormValues(json, DEFAULT_BUILDER_FORM_VALUES);
          } catch (e) {
            setStoredEditorView("yaml");
            setStoredManifest(json);
            analyticsService.track(Namespace.CONNECTOR_BUILDER, Action.UI_INCOMPATIBLE_YAML_IMPORTED, {
              actionDescription: "A YAML manifest that's incompatible with the Builder UI was imported",
              error_message: e.message,
            });
            createAndNavigate(getConnectorName(fileName), json);
            return;
          }

          convertedFormValues.global.connectorName = getConnectorName(fileName, convertedFormValues);
          setStoredEditorView("ui");
          setStoredFormValues(convertedFormValues);
          analyticsService.track(Namespace.CONNECTOR_BUILDER, Action.UI_COMPATIBLE_YAML_IMPORTED, {
            actionDescription: "A YAML manifest that's compatible with the Builder UI was imported",
          });
          createAndNavigate(getConnectorName(fileName), json);
        } finally {
          if (fileInputRef.current) {
            fileInputRef.current.value = "";
          }
          setImportYamlLoading(false);
        }
      };

      if (file) {
        reader.readAsText(file);
      }
    },
    [
      analyticsService,
      convertToBuilderFormValues,
      createAndNavigate,
      registerNotification,
      setStoredEditorView,
      setStoredFormValues,
      setStoredManifest,
    ]
  );

  // clear out notification on unmount, so it doesn't persist after a redirect
  useEffect(() => {
    return () => {
      unregisterNotificationById(YAML_UPLOAD_ERROR_ID);
      unregisterNotificationById(CREATE_PROJECT_ERROR_ID);
    };
  }, [unregisterNotificationById]);

  const isLoading = isCreateProjectLoading || importYamlLoading;

  return (
    <FlexContainer direction="column" alignItems="center" gap="2xl">
      <FlexContainer direction="column" gap="md" alignItems="center" className={styles.titleContainer}>
        <AirbyteLogo />
        <Heading as="h1" size="lg" className={styles.title}>
          <FormattedMessage id="connectorBuilder.title" />
        </Heading>
      </FlexContainer>
      <Heading as="h1" size="lg">
        <FormattedMessage id="connectorBuilder.createPage.prompt" />
      </Heading>
      <FlexContainer direction="row" gap="2xl">
        <input type="file" accept=".yml,.yaml" ref={fileInputRef} onChange={handleYamlUpload} hidden />
        <Tile
          image={<ImportYamlImage />}
          title="connectorBuilder.createPage.importYaml.title"
          description="connectorBuilder.createPage.importYaml.description"
          buttonText="connectorBuilder.createPage.importYaml.button"
          buttonProps={{ isLoading: activeTile === "yaml" && isLoading, disabled: isLoading }}
          onClick={() => {
            unregisterNotificationById(YAML_UPLOAD_ERROR_ID);
            setActiveTile("yaml");
            fileInputRef.current?.click();
          }}
          dataTestId="import-yaml"
        />
        <Tile
          image={<StartFromScratchImage />}
          title="connectorBuilder.createPage.startFromScratch.title"
          description="connectorBuilder.createPage.startFromScratch.description"
          buttonText="connectorBuilder.createPage.startFromScratch.button"
          buttonProps={{ isLoading: activeTile === "empty" && isLoading, disabled: isLoading }}
          onClick={() => {
            setStoredEditorView("ui");
            setActiveTile("empty");
            analyticsService.track(Namespace.CONNECTOR_BUILDER, Action.START_FROM_SCRATCH, {
              actionDescription: "User selected Start From Scratch on the Connector Builder create page",
            });
            createAndNavigate(getConnectorName());
          }}
          dataTestId="start-from-scratch"
        />
      </FlexContainer>
    </FlexContainer>
  );
};

function getConnectorName(fileName?: string | undefined, formValues?: BuilderFormValues) {
  if (!fileName) {
    return DEFAULT_CONNECTOR_NAME;
  }
  const fileNameNoType = lowerCase(fileName.split(".")[0].trim());
  if (fileNameNoType === "manifest" && formValues) {
    // remove http protocol from beginning of url
    return formValues.global.urlBase.replace(/(^\w+:|^)\/\//, "");
  }
  return startCase(fileNameNoType);
}

export const ConnectorBuilderCreatePage: React.FC = () => (
  <ConnectorBuilderLocalStorageProvider>
    <HeadTitle titles={[{ id: "connectorBuilder.title" }]} />
    <ConnectorBuilderCreatePageInner />
  </ConnectorBuilderLocalStorageProvider>
);

interface TileProps {
  image: React.ReactNode;
  title: string;
  description: string;
  buttonText: string;
  buttonProps?: Partial<ButtonProps>;
  onClick: () => void;
  dataTestId: string;
}

const Tile: React.FC<TileProps> = ({ image, title, description, buttonText, buttonProps, onClick, dataTestId }) => {
  return (
    <Card className={styles.tile}>
      <FlexContainer direction="column" gap="xl" alignItems="center">
        <FlexContainer justifyContent="center" className={styles.tileImage}>
          {image}
        </FlexContainer>
        <FlexContainer direction="column" alignItems="center" gap="md" className={styles.tileText}>
          <Heading as="h2" size="sm" centered>
            <FormattedMessage id={title} />
          </Heading>
          <FlexContainer direction="column" justifyContent="center" className={styles.tileDescription}>
            <Text centered>
              <FormattedMessage id={description} />
            </Text>
          </FlexContainer>
        </FlexContainer>
        <Button onClick={onClick} {...buttonProps} data-testid={dataTestId}>
          <FlexContainer direction="row" alignItems="center" gap="md" className={styles.tileButton}>
            <FontAwesomeIcon icon={faArrowRight} />
            <FormattedMessage id={buttonText} />
          </FlexContainer>
        </Button>
      </FlexContainer>
    </Card>
  );
};
